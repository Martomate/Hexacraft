package hexacraft.client.render

import hexacraft.client.ClientWorld
import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.world.Camera

import org.joml.Vector3f

import scala.concurrent.ExecutionContext

trait TerrainRenderer {
  def onTotalSizeChanged(totalSize: Int): Unit
  def onProjMatrixChanged(camera: Camera): Unit
  def regularChunkBufferFragmentation: IndexedSeq[Float]
  def transmissiveChunkBufferFragmentation: IndexedSeq[Float]
  def renderQueueLength: Int
  def render(camera: Camera, sun: Vector3f, opaque: Boolean): Unit
  def tick(camera: Camera, renderDistance: Double, worldTickResult: WorldTickResult)(using ExecutionContext): Unit
  def unload(): Unit
}
