package hexacraft.client

import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.client.render.*
import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, TextureArray, TextureSingle, VAO}
import hexacraft.shaders.*
import hexacraft.util.{NamedThreadFactory, TickableTimer}
import hexacraft.world.*
import hexacraft.world.chunk.{Chunk, ChunkColumnHeightMap, ChunkColumnTerrain, ChunkStorage}
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld}
import hexacraft.world.entity.{Entity, EntityModel}

import org.joml.{Vector2ic, Vector3f}
import org.lwjgl.BufferUtils

import java.nio.ByteBuffer
import java.util.concurrent.Executors
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

class WorldRenderer(
    world: BlocksInWorld,
    worldGenerator: WorldGenerator,
    blockTextureIndices: Map[String, IndexedSeq[Int]],
    blockTextureColors: Map[String, IndexedSeq[Vector3f]],
    initialFrameBufferSize: Vector2ic
)(using CylinderSize) {
  private val executorService = Executors.newFixedThreadPool(8, NamedThreadFactory("render"))
  given ExecutionContext = ExecutionContext.fromExecutor(executorService)

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
  private val transmissiveBlockGpuState = GpuState.build(_.blend(true).cullFace(true))

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

  private var currentlySelectedBlockAndSide: Option[MousePickerResult] = None

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
    val blockDataToUpdate = handleChunkUpdateQueue(camera, renderDistance, worldTickResult.chunksNeedingRenderUpdate)

    // Step 3: Perform render updates using data calculated in the background since the previous frame
    updateBlockData(blockDataToUpdate)

    // performTerrainUpdates(camera)
  }

  private def handleChunkUpdateQueue(
      camera: Camera,
      renderDistance: Double,
      chunksNeedingRenderUpdate: Seq[ChunkRelWorld]
  ): collection.Seq[(ChunkRelWorld, ChunkRenderData)] = {
    // Step 1: Collect render data calculated since the previous frame (used in step 3)
    val (blockDataToUpdate, blockDataNotReady) = {
      // Only collect the completed tasks in the beginning (to keep the order of the updates)
      val idxFirstPending = futureRenderData.indexWhere(!_._2.isCompleted)
      val (completed, rest) = if idxFirstPending != -1 then {
        futureRenderData.splitAt(idxFirstPending)
      } else {
        (futureRenderData, new ArrayBuffer(0))
      }
      (completed.map((coords, fut) => coords -> fut.value.get.get), rest)
    }
    futureRenderData.clear()
    futureRenderData ++= blockDataNotReady
    val chunksAlreadyUpdating = futureRenderData.filter(!_._2.isCompleted).map(_._1)
    val numUpdatesPending = chunksAlreadyUpdating.size

    // Step 2: Start calculating render updates in the background
    for coords <- chunksNeedingRenderUpdate do {
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

    val chunkRenderUpdatesSkipped = mutable.ArrayBuffer.empty[ChunkRelWorld]

    var numUpdatesToPerform = math.min(chunkRenderUpdateQueue.length, math.max(15 - numUpdatesPending, 0))
    while numUpdatesToPerform > 0 do {
      chunkRenderUpdateQueue.pop() match {
        case Some(coords) =>
          world.getChunk(coords) match {
            case Some(chunk) =>
              if coords.neighbors.forall(n => world.getChunk(n).isDefined) then {
                if !chunksAlreadyUpdating.contains(coords) then {
                  futureRenderData += coords -> Future(
                    ChunkRenderData(coords, chunk.blocks, world, blockTextureIndices)
                  )
                  numUpdatesToPerform -= 1
                } else {
                  chunkRenderUpdatesSkipped += coords
                }
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

    for coords <- chunkRenderUpdatesSkipped do {
      chunkRenderUpdateQueue.insert(coords)
    }

    blockDataToUpdate
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

  def render(camera: Camera, sun: Vector3f, selectedBlockAndSide: Option[MousePickerResult]): Unit = {
    if currentlySelectedBlockAndSide != selectedBlockAndSide then {
      currentlySelectedBlockAndSide = selectedBlockAndSide

      selectedBlockAndSide match {
        case Some(MousePickerResult(state, coords, Some(_))) =>
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

    // World content
    renderBlocks(camera, sun)
    // TODO: Opaque and translucent blocks are both rendered here, but they need to be separate.
    //  In the world combiner there is a normal field, but if there is glass in front of grass then the normal will be for the glass.
    //  The normal-based shading only happens for the grass.
    //  Instead we should render in two steps, or alternatively send both to the shader. How is this usually done?

    // renderTerrain(camera, sun)
    renderEntities(camera, sun)

    if selectedBlockAndSide.flatMap(_.side).isDefined then {
      renderSelectedBlock(camera)
    }

    mainFrameBuffer.unbind()
    OpenGL.glViewport(0, 0, mainFrameBuffer.frameBuffer.width, mainFrameBuffer.frameBuffer.height)

    OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)

    renderSky(camera, sun)

    // Step 2: Render the FrameBuffer to the screen (one could add post processing here in the future)
    OpenGL.glActiveTexture(worldCombinerShader.positionTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.positionTexture)
    OpenGL.glActiveTexture(worldCombinerShader.normalTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.normalTexture)
    OpenGL.glActiveTexture(worldCombinerShader.colorTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.colorTexture)
    OpenGL.glActiveTexture(worldCombinerShader.depthTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.depthTexture)

    worldCombinerShader.enable()
    worldCombinerShader.setSunPosition(sun)
    worldCombinerRenderer.render(worldCombinerVao, worldCombinerVao.maxCount)

    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(3))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(2))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
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

  private def renderSelectedBlock(camera: Camera): Unit = {
    selectedBlockShader.setViewMatrix(camera.view.matrix)
    selectedBlockShader.setCameraPosition(camera.position)
    selectedBlockShader.enable()
    selectedBlockRenderer.render(selectedBlockVao, 1)
  }

  private def renderBlocks(camera: Camera, sun: Vector3f): Unit = {
    blockShader.setViewMatrix(camera.view.matrix)
    blockShader.setCameraPosition(camera.position)

    blockSideShader.setViewMatrix(camera.view.matrix)
    blockSideShader.setCameraPosition(camera.position)

    blockTexture.bind()
    renderBlocks()
  }

  private def renderTerrain(camera: Camera, sun: Vector3f): Unit = {
    terrainShader.setViewMatrix(camera.view.matrix)
    terrainShader.setCameraPosition(camera.position)
    terrainShader.setSunPosition(sun)

    renderTerrain()
  }

  private def updateBlockData(chunks: collection.Seq[(ChunkRelWorld, ChunkRenderData)]): Unit = {
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
      chunksToClear: collection.Seq[ChunkRelWorld],
      chunksToUpdate: collection.Seq[(ChunkRelWorld, IndexedSeq[ByteBuffer])],
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

    entitySideShader.setViewMatrix(camera.view.matrix)
    entitySideShader.setCameraPosition(camera.position)

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

    for r <- terrainRenderers.values do {
      r.unload()
    }

    blockShader.free()
    blockSideShader.free()
    terrainShader.free()

    mainFrameBuffer.unload()

    executorService.shutdown()
  }
}
