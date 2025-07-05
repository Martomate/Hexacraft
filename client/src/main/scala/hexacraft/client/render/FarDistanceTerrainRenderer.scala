package hexacraft.client.render

import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.renderer.GpuState
import hexacraft.shaders.TerrainShader
import hexacraft.world.*
import hexacraft.world.chunk.{ChunkColumnHeightMap, ChunkColumnTerrain, ChunkStorage}
import hexacraft.world.coord.{ChunkRelWorld, ColumnRelWorld, CylCoords}

import org.joml.Vector3f

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.concurrent.ExecutionContext

class FarDistanceTerrainRenderer(worldGenerator: WorldGenerator, blockTextureColors: Map[String, IndexedSeq[Vector3f]])(
    using CylinderSize
) extends TerrainRenderer {
  private val terrainShader = new TerrainShader()
  private val terrainRenderers: mutable.LongMap[TerrainBatchRenderer] = mutable.LongMap.empty
  private val terrainLoadingPrio = new ChunkLoadingPrioritizer(20)
  private val columnCache: mutable.LongMap[ChunkColumnTerrain] = mutable.LongMap.empty
  private val chunkCache: mutable.LongMap[ChunkStorage] = mutable.LongMap.empty
  private val terrainGpuState = GpuState.build(_.blend(false).cullFace(true))

  override def regularChunkBufferFragmentation: IndexedSeq[Float] = IndexedSeq.empty

  override def transmissiveChunkBufferFragmentation: IndexedSeq[Float] = IndexedSeq.empty

  override def renderQueueLength: Int = 0

  override def render(camera: Camera, sun: Vector3f, opaque: Boolean): Unit = {
    renderTerrain(camera, sun)
  }

  override def tick(camera: Camera, renderDistance: Double, worldTickResult: WorldTickResult)(using
      ExecutionContext
  ): Unit = {
    performTerrainUpdates(camera)
  }

  private def performTerrainUpdates(camera: Camera): Unit = {
    val terrainUpdates = mutable.ArrayBuffer.empty[(ChunkRelWorld, ByteBuffer)]
    terrainLoadingPrio.tick(Pose(CylCoords(camera.view.position), camera.view.forward))

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
              columnCache.getOrElseUpdate(col.value, ChunkColumnTerrain.create(col, worldGenerator, None))
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

  private def makeTerrainBufferHandler(): BufferHandler[?] =
    new BufferHandler(
      100000 * 3,
      TerrainShader.bytesPerVertex,
      VaoRenderBuffer.Allocator(TerrainShader.createVao, terrainGpuState)
    )

  private def chunkGroup(c: ChunkRelWorld) = {
    ChunkRelWorld(c.X.toInt & ~7, c.Y.toInt & ~7, c.Z.toInt & ~7).value
  }

  private def renderTerrain(camera: Camera, sun: Vector3f): Unit = {
    terrainShader.setViewMatrix(camera.view.matrix)
    terrainShader.setCameraPosition(camera.position)
    terrainShader.setSunPosition(sun)

    renderTerrain()
  }

  private def renderTerrain(): Unit = {
    val sh = terrainShader
    sh.enable()
    for r <- terrainRenderers.values do {
      r.render()
    }
  }

  def onTotalSizeChanged(totalSize: Int): Unit = {
    terrainShader.setTotalSize(totalSize)
  }

  def onProjMatrixChanged(camera: Camera): Unit = {
    terrainShader.setProjectionMatrix(camera.proj.matrix)
  }

  def unload(): Unit = {
    for r <- terrainRenderers.values do {
      r.unload()
    }

    terrainShader.free()
  }
}
