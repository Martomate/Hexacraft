package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, TextureArray, VAO}
import hexacraft.shaders.{BlockShader, EntityShader, SelectedBlockShader, SkyShader, WorldCombinerShader}
import hexacraft.util.TickableTimer
import hexacraft.world.{BlocksInWorld, Camera, CylinderSize, World}
import hexacraft.world.World.WorldTickResult
import hexacraft.world.block.BlockState
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.{Entity, EntityModel}

import org.joml.{Vector2ic, Vector3f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class WorldRenderer(
    world: BlocksInWorld,
    blockTextureIndices: Map[String, IndexedSeq[Int]],
    initialFrameBufferSize: Vector2ic
)(using CylinderSize) {

  private val skyShader = new SkyShader()
  private val entityShader = new EntityShader(isSide = false)
  private val entitySideShader = new EntityShader(isSide = true)
  private val selectedBlockShader = new SelectedBlockShader()
  private val worldCombinerShader = new WorldCombinerShader()

  private val blockShader = new BlockShader(isSide = false)
  private val blockSideShader = new BlockShader(isSide = true)
  private val blockTexture = TextureArray.getTextureArray("blocks")

  private val solidBlockGpuState = GpuState.build(_.blend(false).cullFace(true))
  private val transmissiveBlockGpuState = GpuState.build(_.blend(true).cullFace(false))

  private val solidBlockRenderers: IndexedSeq[mutable.LongMap[BlockFaceBatchRenderer]] =
    IndexedSeq.tabulate(8)(s => new mutable.LongMap())
  private val transmissiveBlockRenderers: IndexedSeq[mutable.LongMap[BlockFaceBatchRenderer]] =
    IndexedSeq.tabulate(8)(s => new mutable.LongMap())

  private val skyVao: VAO = SkyShader.createVao()
  private val skyRenderer = SkyShader.createRenderer()

  private val worldCombinerVao: VAO = WorldCombinerShader.createVao()
  private val worldCombinerRenderer = WorldCombinerShader.createRenderer()

  private val selectedBlockVao = SelectedBlockShader.createVao()
  private val selectedBlockRenderer = SelectedBlockShader.createRenderer()

  private var mainFrameBuffer = MainFrameBuffer.fromSize(initialFrameBufferSize.x, initialFrameBufferSize.y)

  private var currentlySelectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])] = None

  private val chunksToRender: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val entityRenderers = for s <- 0 until 8 yield BlockRenderer(EntityShader.createVao(s), GpuState())

  private val chunkRenderUpdateQueue: ChunkRenderUpdateQueue = new ChunkRenderUpdateQueue
  private val chunkRenderUpdateQueueReorderingTimer: TickableTimer = TickableTimer(5) // only reorder every 5 ticks

  private val players = ArrayBuffer.empty[Entity]

  def addPlayer(player: Entity): Unit = {
    players += player
  }

  def removePlayer(player: Entity): Unit = {
    players -= player
  }

  def regularChunkBufferFragmentation: IndexedSeq[Float] =
    solidBlockRenderers.map(a => a.values.map(_.fragmentation).sum / a.keys.size)

  def transmissiveChunkBufferFragmentation: IndexedSeq[Float] =
    transmissiveBlockRenderers.map(a => a.values.map(_.fragmentation).sum / a.keys.size)

  def tick(camera: Camera, renderDistance: Double, worldTickResult: WorldTickResult): Unit = {
    chunksToRender ++= worldTickResult.chunksAdded
    chunksToRender --= worldTickResult.chunksRemoved

    for coords <- worldTickResult.chunksNeedingRenderUpdate do {
      chunkRenderUpdateQueue.insert(coords)
    }

    if chunkRenderUpdateQueueReorderingTimer.tick() then {
      chunkRenderUpdateQueue.reorderAndFilter(camera, renderDistance)
    }

    val updatedChunkData = mutable.ArrayBuffer.empty[(ChunkRelWorld, ChunkRenderData)]
    for coords <- worldTickResult.chunksRemoved do {
      updatedChunkData += coords -> ChunkRenderData.empty
    }

    var numUpdatesToPerform = math.min(chunkRenderUpdateQueue.length, 4)
    while numUpdatesToPerform > 0 do {
      chunkRenderUpdateQueue.pop() match {
        case Some(coords) =>
          world.getChunk(coords) match {
            case Some(chunk) =>
              val renderData = ChunkRenderData(coords, chunk.blocks, world, blockTextureIndices)
              updatedChunkData += coords -> renderData
              numUpdatesToPerform -= 1
            case None =>
          }
        case None =>
          numUpdatesToPerform = 0
      }
    }

    updateBlockData(updatedChunkData.toSeq)
  }

  def onTotalSizeChanged(totalSize: Int): Unit = {
    blockShader.setTotalSize(totalSize)
    blockSideShader.setTotalSize(totalSize)

    entityShader.setTotalSize(totalSize)
    entitySideShader.setTotalSize(totalSize)
    selectedBlockShader.setTotalSize(totalSize)
  }

  def onProjMatrixChanged(camera: Camera): Unit = {
    blockShader.setProjectionMatrix(camera.proj.matrix)
    blockSideShader.setProjectionMatrix(camera.proj.matrix)

    entityShader.setProjectionMatrix(camera.proj.matrix)
    entitySideShader.setProjectionMatrix(camera.proj.matrix)
    selectedBlockShader.setProjectionMatrix(camera.proj.matrix)

    skyShader.setInverseProjectionMatrix(camera.proj.invMatrix)

    worldCombinerShader.setClipPlanes(camera.proj.near, camera.proj.far)
  }

  def render(
      camera: Camera,
      sun: Vector3f,
      selectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])]
  ): Unit = {
    if currentlySelectedBlockAndSide != selectedBlockAndSide then {
      currentlySelectedBlockAndSide = selectedBlockAndSide

      selectedBlockAndSide match {
        case Some((state, coords, Some(_))) =>
          val buf = BufferUtils.createByteBuffer(7 * 4)
          SelectedBlockShader.InstanceData(coords, state).fill(buf)
          buf.flip()
          selectedBlockVao.vbos(1).fill(0, buf)
        case _ =>
      }
    }

    // Step 1: Render everything to a FrameBuffer
    mainFrameBuffer.bind()

    OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)

    renderSky(camera, sun)

    // World content
    renderBlocks(camera, sun)
    renderEntities(camera, sun)

    if selectedBlockAndSide.flatMap(_._3).isDefined then {
      renderSelectedBlock(camera, selectedBlockAndSide)
    }

    mainFrameBuffer.unbind()
    OpenGL.glViewport(0, 0, mainFrameBuffer.frameBuffer.width, mainFrameBuffer.frameBuffer.height)

    // Step 2: Render the FrameBuffer to the screen (one could add post processing here in the future)
    OpenGL.glActiveTexture(worldCombinerShader.colorTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.colorTexture)
    OpenGL.glActiveTexture(worldCombinerShader.depthTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.depthTexture)

    worldCombinerShader.enable()
    worldCombinerRenderer.render(worldCombinerVao, worldCombinerVao.maxCount)

    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(1))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(0))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
  }

  private def renderSky(camera: Camera, sun: Vector3f): Unit = {
    skyShader.setInverseViewMatrix(camera.view.invMatrix)
    skyShader.setSunPosition(sun)
    skyShader.enable()
    skyRenderer.render(skyVao, skyVao.maxCount)
  }

  private def renderSelectedBlock(
      camera: Camera,
      selectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])]
  ): Unit = {
    selectedBlockShader.setViewMatrix(camera.view.matrix)
    selectedBlockShader.setCameraPosition(camera.position)
    selectedBlockShader.enable()
    selectedBlockRenderer.render(selectedBlockVao, 1)
  }

  private def renderBlocks(camera: Camera, sun: Vector3f): Unit = {
    blockShader.setViewMatrix(camera.view.matrix)
    blockShader.setCameraPosition(camera.position)
    blockShader.setSunPosition(sun)

    blockSideShader.setViewMatrix(camera.view.matrix)
    blockSideShader.setCameraPosition(camera.position)
    blockSideShader.setSunPosition(sun)

    blockTexture.bind()
    renderBlocks()
  }

  private def updateBlockData(chunks: Seq[(ChunkRelWorld, ChunkRenderData)]): Unit = {
    val opaqueData = chunks.partitionMap((coords, data) =>
      data.opaqueBlocks match {
        case Some(content) => Right((coords, content))
        case None          => Left(coords)
      }
    )
    updateBlockData(opaqueData._1, opaqueData._2, transmissive = false)

    val transmissiveData = chunks.partitionMap((coords, data) =>
      data.transmissiveBlocks match {
        case Some(content) => Right((coords, content))
        case None          => Left(coords)
      }
    )
    updateBlockData(transmissiveData._1, transmissiveData._2, transmissive = true)
  }

  private def renderBlocks(): Unit = {
    for side <- 0 until 8 do {
      val sh = if side < 2 then blockShader else blockSideShader
      sh.enable()
      sh.setSide(side)
      for h <- solidBlockRenderers(side).values do {
        h.render()
      }
    }

    for side <- 0 until 8 do {
      val sh = if side < 2 then blockShader else blockSideShader
      sh.enable()
      sh.setSide(side)
      for h <- transmissiveBlockRenderers(side).values do {
        h.render()
      }
    }
  }

  private def makeBufferHandler(s: Int, gpuState: GpuState): BufferHandler[?] =
    new BufferHandler(100000 * 3, BlockShader.bytesPerVertex(s), VaoRenderBuffer.Allocator(s, gpuState))

  private def updateBlockData(
      chunksToClear: Seq[ChunkRelWorld],
      chunksToUpdate: Seq[(ChunkRelWorld, IndexedSeq[ByteBuffer])],
      transmissive: Boolean
  ): Unit = {
    val clear = chunksToClear.groupBy(c => chunkGroup(c))
    val update = chunksToUpdate.groupBy((c, _) => chunkGroup(c))

    val groups = (clear.keys ++ update.keys).toSet
    val groupData = for g <- groups yield {
      (g, clear.getOrElse(g, Seq()), update.getOrElse(g, Seq()))
    }

    val gpuState = if transmissive then transmissiveBlockGpuState else solidBlockGpuState

    for s <- 0 until 8 do {
      val batchRenderers = if transmissive then transmissiveBlockRenderers(s) else solidBlockRenderers(s)

      for (g, clear, update) <- groupData do {
        val r = batchRenderers.getOrElseUpdate(g, new BlockFaceBatchRenderer(makeBufferHandler(s, gpuState)))

        r.update(clear, update.map((c, data) => c -> data(s)))

        if r.isEmpty then {
          r.unload()
          batchRenderers.remove(g)
        }
      }
    }
  }

  private def chunkGroup(c: ChunkRelWorld) = {
    ChunkRelWorld(c.X.toInt & ~7, c.Y.toInt & ~7, c.Z.toInt & ~7).value
  }

  def frameBufferResized(width: Int, height: Int): Unit = {
    val newFrameBuffer = MainFrameBuffer.fromSize(width, height)
    mainFrameBuffer.unload()
    mainFrameBuffer = newFrameBuffer
  }

  private def renderEntities(camera: Camera, sun: Vector3f): Unit = {
    entityShader.setViewMatrix(camera.view.matrix)
    entityShader.setCameraPosition(camera.position)
    entityShader.setSunPosition(sun)

    entitySideShader.setViewMatrix(camera.view.matrix)
    entitySideShader.setCameraPosition(camera.position)
    entitySideShader.setSunPosition(sun)

    for side <- 0 until 8 do {
      val sh = if side < 2 then entityShader else entitySideShader
      sh.enable()
      sh.setSide(side)

      val entityDataList: mutable.ArrayBuffer[(EntityModel, Seq[EntityShader.InstanceData])] = ArrayBuffer.empty
      for {
        c <- chunksToRender
        ch <- world.getChunk(c)
      } do {
        val entities = ch.entities
        if entities.nonEmpty then {
          val data = EntityRenderDataFactory.getEntityRenderData(entities, side, world)
          entityDataList ++= data
        }
      }

      entityDataList ++= EntityRenderDataFactory.getEntityRenderData(players, side, world)

      for (texture, partLists) <- entityDataList.groupBy(_._1.texture) do {
        val data = partLists.flatMap(_._2)

        entityRenderers(side).setInstanceData(data.size): buf =>
          data.foreach(_.fill(buf))

        texture.bind()
        sh.setTextureSize(texture.width)
        entityRenderers(side).render()
      }
    }
  }

  def unload(): Unit = {
    skyVao.free()
    skyShader.free()
    selectedBlockVao.free()
    selectedBlockShader.free()
    worldCombinerVao.free()
    worldCombinerShader.free()
    entityShader.free()
    entitySideShader.free()

    for r <- entityRenderers do {
      r.unload()
    }

    for rs <- solidBlockRenderers do {
      for h <- rs.values do {
        h.unload()
      }
      rs.clear()
    }

    for rs <- transmissiveBlockRenderers do {
      for h <- rs.values do {
        h.unload()
      }
      rs.clear()
    }

    blockShader.free()
    blockSideShader.free()

    mainFrameBuffer.unload()
  }
}
