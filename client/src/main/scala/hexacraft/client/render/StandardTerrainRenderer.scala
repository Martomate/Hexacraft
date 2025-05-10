package hexacraft.client.render

import hexacraft.client.ClientWorld
import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.renderer.{GpuState, TextureArray}
import hexacraft.shaders.BlockShader
import hexacraft.util.{InlinedIterable, Loop, TickableTimer}
import hexacraft.world.{Camera, CylinderSize}
import hexacraft.world.coord.ChunkRelWorld

import org.joml.Vector3f

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

class StandardTerrainRenderer(world: ClientWorld, blockTextureIndices: Map[String, IndexedSeq[Int]])(using
    CylinderSize
) extends TerrainRenderer {
  private val opaqueBlockGpuState = GpuState.build(_.blend(false).cullFace(true))
  private val translucentBlockGpuState = GpuState.build(_.blend(true).cullFace(true))
  private val futureRenderData: ArrayBuffer[(ChunkRelWorld, Future[ChunkRenderData])] = ArrayBuffer.empty
  private val opaqueBlockRenderers: IndexedSeq[mutable.LongMap[BlockFaceBatchRenderer]] =
    IndexedSeq.tabulate(8)(s => new mutable.LongMap())
  private val translucentBlockRenderers: IndexedSeq[mutable.LongMap[BlockFaceBatchRenderer]] =
    IndexedSeq.tabulate(8)(s => new mutable.LongMap())
  private val chunkRenderUpdateQueue: ChunkRenderUpdateQueue = new ChunkRenderUpdateQueue
  private val chunkRenderUpdateQueueReorderingTimer: TickableTimer = TickableTimer(5) // only reorder every 5 ticks
  private val blockShader = new BlockShader(isSide = false)
  private val blockSideShader = new BlockShader(isSide = true)
  private val blockTexture = TextureArray.getTextureArray("blocks")

  override def onTotalSizeChanged(totalSize: Int): Unit = {
    blockShader.setTotalSize(totalSize)
    blockSideShader.setTotalSize(totalSize)
  }

  override def onProjMatrixChanged(camera: Camera): Unit = {
    blockShader.setProjectionMatrix(camera.proj.matrix)
    blockSideShader.setProjectionMatrix(camera.proj.matrix)
  }

  override def regularChunkBufferFragmentation: IndexedSeq[Float] =
    opaqueBlockRenderers.map(a => a.values.map(_.fragmentation).sum / a.keys.size)

  override def transmissiveChunkBufferFragmentation: IndexedSeq[Float] =
    translucentBlockRenderers.map(a => a.values.map(_.fragmentation).sum / a.keys.size)

  override def renderQueueLength: Int =
    chunkRenderUpdateQueue.length

  override def render(camera: Camera, sun: Vector3f, opaque: Boolean): Unit = {
    renderBlocks(camera, sun, opaque)
  }

  override def tick(camera: Camera, renderDistance: Double, worldTickResult: WorldTickResult)(using
      ExecutionContext
  ): Unit = {
    val blockDataToUpdate = handleChunkUpdateQueue(camera, renderDistance, worldTickResult.chunksNeedingRenderUpdate)

    // Step 3: Perform render updates using data calculated in the background since the previous frame
    updateBlockData(blockDataToUpdate)
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

  private def chunkGroup(c: ChunkRelWorld) = {
    ChunkRelWorld(c.X.toInt & ~7, c.Y.toInt & ~7, c.Z.toInt & ~7).value
  }

  private def updateBlockData(
      chunksToClear: collection.Seq[ChunkRelWorld],
      chunksToUpdate: collection.Seq[(ChunkRelWorld, IndexedSeq[ByteBuffer])],
      transmissive: Boolean
  ): Unit = {
    val clear = chunksToClear.groupBy(c => chunkGroup(c))
    val update = chunksToUpdate.groupBy((c, _) => chunkGroup(c))

    val groups = (clear.keys ++ update.keys).toSet
    val groupData = for g <- InlinedIterable(groups) yield {
      (g, clear.getOrElse(g, Seq()), update.getOrElse(g, Seq()))
    }

    val gpuState = if transmissive then translucentBlockGpuState else opaqueBlockGpuState

    Loop.rangeUntil(0, 8) { s =>
      val batchRenderers = if transmissive then translucentBlockRenderers(s) else opaqueBlockRenderers(s)

      for (g, clear, update) <- InlinedIterable(groupData) do {
        val r = batchRenderers.getOrElseUpdate(g, new BlockFaceBatchRenderer(makeBufferHandler(s, gpuState)))

        r.update(clear, update.map((c, data) => c -> data(s)))

        if r.isEmpty then {
          r.unload()
          batchRenderers.remove(g)
        }
      }
    }
  }

  private def makeBufferHandler(s: Int, gpuState: GpuState): BufferHandler[?] =
    new BufferHandler(
      100000 * 3,
      BlockShader.bytesPerVertex(s),
      VaoRenderBuffer.Allocator(numVertices => BlockShader.createVao(s, numVertices), gpuState)
    )

  private def handleChunkUpdateQueue(
      camera: Camera,
      renderDistance: Double,
      chunksNeedingRenderUpdate: Seq[ChunkRelWorld]
  )(using ExecutionContext): collection.Seq[(ChunkRelWorld, ChunkRenderData)] = {
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

  private def renderBlocks(camera: Camera, sun: Vector3f, opaque: Boolean): Unit = {
    blockShader.setViewMatrix(camera.view.matrix)
    blockShader.setCameraPosition(camera.position)

    blockSideShader.setViewMatrix(camera.view.matrix)
    blockSideShader.setCameraPosition(camera.position)

    blockTexture.bind()
    renderBlocks(opaque)
  }

  private def renderBlocks(opaque: Boolean): Unit = {
    if opaque then {
      Loop.rangeUntil(0, 8) { side =>
        val sh = if side < 2 then blockShader else blockSideShader
        sh.enable()
        sh.setSide(side)
        for h <- InlinedIterable(opaqueBlockRenderers(side).values) do {
          h.render()
        }
      }
    } else {
      Loop.rangeUntil(0, 8) { side =>
        val sh = if side < 2 then blockShader else blockSideShader
        sh.enable()
        sh.setSide(side)
        for h <- InlinedIterable(translucentBlockRenderers(side).values) do {
          h.render()
        }
      }
    }
  }

  override def unload(): Unit = {
    for rs <- opaqueBlockRenderers do {
      for h <- rs.values do {
        h.unload()
      }
      rs.clear()
    }

    for rs <- translucentBlockRenderers do {
      for h <- rs.values do {
        h.unload()
      }
      rs.clear()
    }

    blockShader.free()
    blockSideShader.free()
  }
}
