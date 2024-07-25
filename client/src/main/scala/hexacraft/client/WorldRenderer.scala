package hexacraft.client

import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.client.render.*
import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, TextureArray, TextureSingle, VAO}
import hexacraft.shaders.*
import hexacraft.util.TickableTimer
import hexacraft.world.{BlocksInWorld, Camera, ChunkLoadingPrioritizer, CylinderSize, PosAndDir, WorldGenerator}
import hexacraft.world.block.BlockState
import hexacraft.world.chunk.{Chunk, ChunkColumnHeightMap, ChunkColumnTerrain, ChunkStorage}
import hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, ColumnRelWorld}
import hexacraft.world.entity.{Entity, EntityModel}

import org.joml.{Vector2ic, Vector3f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class WorldRenderer(
    world: BlocksInWorld,
    worldGenerator: WorldGenerator,
    blockTextureIndices: Map[String, IndexedSeq[Int]],
    blockTextureColors: Map[String, IndexedSeq[Vector3f]],
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

  private val terrainShader = new TerrainShader()

  private val solidBlockGpuState = GpuState.build(_.blend(false).cullFace(true))
  private val transmissiveBlockGpuState = GpuState.build(_.blend(true).cullFace(false))

  private val terrainGpuState = GpuState.build(_.blend(false).cullFace(true))

  private val solidBlockRenderers: IndexedSeq[mutable.LongMap[BlockFaceBatchRenderer]] =
    IndexedSeq.tabulate(8)(s => new mutable.LongMap())
  private val transmissiveBlockRenderers: IndexedSeq[mutable.LongMap[BlockFaceBatchRenderer]] =
    IndexedSeq.tabulate(8)(s => new mutable.LongMap())

  private val terrainRenderers: mutable.LongMap[TerrainBatchRenderer] = mutable.LongMap.empty

  private val skyVao: VAO = SkyShader.createVao()
  private val skyRenderer = SkyShader.createRenderer()

  private val worldCombinerVao: VAO = WorldCombinerShader.createVao()
  private val worldCombinerRenderer = WorldCombinerShader.createRenderer()

  private val selectedBlockVao = SelectedBlockShader.createVao()
  private val selectedBlockRenderer = SelectedBlockShader.createRenderer()

  private var mainFrameBuffer = MainFrameBuffer.fromSize(initialFrameBufferSize.x, initialFrameBufferSize.y)

  private var currentlySelectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])] = None

  private val entityRenderers = for s <- 0 until 8 yield BlockRenderer(EntityShader.createVao(s), GpuState())

  private val chunkRenderUpdateQueue: ChunkRenderUpdateQueue = new ChunkRenderUpdateQueue
  private val chunkRenderUpdateQueueReorderingTimer: TickableTimer = TickableTimer(5) // only reorder every 5 ticks

  private val futureRenderData: ArrayBuffer[(ChunkRelWorld, Future[ChunkRenderData])] = ArrayBuffer.empty

  private val players = ArrayBuffer.empty[Entity]

  private val terrainLoadingPrio = new ChunkLoadingPrioritizer(20)
  private val columnCache: mutable.LongMap[ChunkColumnTerrain] = mutable.LongMap.empty
  private val chunkCache: mutable.LongMap[ChunkStorage] = mutable.LongMap.empty

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

  def renderQueueLength: Int = {
    chunkRenderUpdateQueue.length
  }

  def tick(camera: Camera, renderDistance: Double, worldTickResult: WorldTickResult): Unit = {
    // Step 1: Perform render updates using data calculated in the background since the previous frame
    updateBlockData(futureRenderData.map((coords, fut) => (coords, Await.result(fut, Duration.Inf))).toSeq)
    futureRenderData.clear()

    // Step 2: Start calculating render updates in the background
    for coords <- worldTickResult.chunksNeedingRenderUpdate do {
      if world.getChunk(coords).isEmpty then {
        // clear the chunk immediately so it doesn't have to be drawn (the PQ is in closest first order)
        futureRenderData += coords -> Future.successful(ChunkRenderData.empty)
      } else if !coords.neighbors.forall(n => world.getChunk(n).isDefined) then {
        // some neighbor has not been loaded yet, so let's not render this chunk yet
        futureRenderData += coords -> Future.successful(ChunkRenderData.empty)
      }
      chunkRenderUpdateQueue.insert(coords)
    }

    if chunkRenderUpdateQueueReorderingTimer.tick() then {
      chunkRenderUpdateQueue.reorderAndFilter(camera, renderDistance)
    }

    var numUpdatesToPerform = math.min(chunkRenderUpdateQueue.length, 15)
    while numUpdatesToPerform > 0 do {
      chunkRenderUpdateQueue.pop() match {
        case Some(coords) =>
          world.getChunk(coords) match {
            case Some(chunk) =>
              if coords.neighbors.forall(n => world.getChunk(n).isDefined) then {
                futureRenderData += coords -> Future(ChunkRenderData(coords, chunk.blocks, world, blockTextureIndices))
                numUpdatesToPerform -= 1
              } else {
                futureRenderData += coords -> Future.successful(ChunkRenderData.empty)
              }
            case None =>
              futureRenderData += coords -> Future.successful(ChunkRenderData.empty)
          }
        case None =>
          numUpdatesToPerform = 0
      }
    }

    // performTerrainUpdates(camera)
  }

  private def performTerrainUpdates(camera: Camera): Unit = {
    val terrainUpdates = mutable.ArrayBuffer.empty[(ChunkRelWorld, ByteBuffer)]
    terrainLoadingPrio.tick(PosAndDir.fromCameraView(camera.view))

    val terrainRemovals = mutable.ArrayBuffer.empty[ChunkRelWorld]
    var doneRemoving = false
    while !doneRemoving do {
      terrainLoadingPrio.popChunkToRemove() match {
        case Some(coords) =>
          terrainRemovals += coords
        case None =>
          doneRemoving = true
      }
    }
    for _ <- 0 until 10 do {
      terrainLoadingPrio.nextAddableChunk match {
        case Some(coords) =>
          val columnCoords = coords.getColumnRelWorld
          val cc = columnCoords

          val columns = mutable.LongMap.empty[ChunkColumnTerrain]
          for {
            dx <- -1 to 1
            dz <- -1 to 1
          } do {
            val col = ColumnRelWorld(cc.X.toInt + dx, cc.Z.toInt + dz)
            columns.put(
              col.value,
              columnCache.getOrElseUpdate(
                col.value,
                ChunkColumnTerrain.create(
                  ChunkColumnHeightMap.fromData2D(worldGenerator.getHeightmapInterpolator(col)),
                  None
                )
              )
            )
          }

          val chunks = mutable.LongMap.empty[ChunkStorage]
          for {
            dx <- -1 to 1
            dy <- -1 to 1
            dz <- -1 to 1
          } do {
            val ch = ChunkRelWorld(coords.X.toInt + dx, coords.Y.toInt + dy, coords.Z.toInt + dz)
            chunks.put(
              ch.value,
              chunkCache.getOrElseUpdate(
                ch.value,
                worldGenerator.generateChunk(ch, columns(ch.getColumnRelWorld.value))
              )
            )
          }

          val data = TerrainVboData.fromChunk(coords, chunks, blockTextureColors)
          if data.hasRemaining then {
            terrainUpdates += coords -> data
          }
          terrainLoadingPrio += coords
        case None =>
      }
    }

    for (g, removals) <- terrainRemovals.toSeq.groupBy(c => chunkGroup(c)) do {
      terrainRenderers
        .getOrElseUpdate(g, new TerrainBatchRenderer(makeTerrainBufferHandler()))
        .update(removals, Seq())
    }

    for (g, updates) <- terrainUpdates.toSeq.groupBy((c, _) => chunkGroup(c)) do {
      terrainRenderers
        .getOrElseUpdate(g, new TerrainBatchRenderer(makeTerrainBufferHandler()))
        .update(Seq(), updates)
    }
  }

  def onTotalSizeChanged(totalSize: Int): Unit = {
    blockShader.setTotalSize(totalSize)
    blockSideShader.setTotalSize(totalSize)
    terrainShader.setTotalSize(totalSize)

    entityShader.setTotalSize(totalSize)
    entitySideShader.setTotalSize(totalSize)
    selectedBlockShader.setTotalSize(totalSize)
  }

  def onProjMatrixChanged(camera: Camera): Unit = {
    blockShader.setProjectionMatrix(camera.proj.matrix)
    blockSideShader.setProjectionMatrix(camera.proj.matrix)
    terrainShader.setProjectionMatrix(camera.proj.matrix)

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
    // renderTerrain(camera, sun)
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

  private def renderTerrain(camera: Camera, sun: Vector3f): Unit = {
    terrainShader.setViewMatrix(camera.view.matrix)
    terrainShader.setCameraPosition(camera.position)
    terrainShader.setSunPosition(sun)

    renderTerrain()
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

  private def renderTerrain(): Unit = {
    val sh = terrainShader
    sh.enable()
    for r <- terrainRenderers.values do {
      r.render()
    }
  }

  private def makeBufferHandler(s: Int, gpuState: GpuState): BufferHandler[?] =
    new BufferHandler(
      100000 * 3,
      BlockShader.bytesPerVertex(s),
      VaoRenderBuffer.Allocator(numVertices => BlockShader.createVao(s, numVertices), gpuState)
    )

  private def makeTerrainBufferHandler(): BufferHandler[?] =
    new BufferHandler(
      100000 * 3,
      TerrainShader.bytesPerVertex,
      VaoRenderBuffer.Allocator(TerrainShader.createVao, terrainGpuState)
    )

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
        c <- world.loadedChunks
        ch <- world.getChunk(c)
      } do {
        if ch.hasEntities then {
          val data = EntityRenderDataFactory.getEntityRenderData(ch.entities, side, world)
          entityDataList ++= data
        }
      }

      entityDataList ++= EntityRenderDataFactory.getEntityRenderData(players, side, world)

      for (textureName, partLists) <- entityDataList.groupBy(_._1.textureName) do {
        val data = partLists.flatMap(_._2)

        entityRenderers(side).setInstanceData(data.size): buf =>
          data.foreach(_.fill(buf))

        val texture = TextureSingle.getTexture("textures/entities/" + textureName)

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
    terrainShader.free()

    mainFrameBuffer.unload()
  }
}
