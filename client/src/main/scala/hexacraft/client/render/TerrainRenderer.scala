package hexacraft.client.render

import hexacraft.client.ClientWorld
import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.world.Camera

import org.joml.{Matrix4f, Vector3f}

import scala.concurrent.ExecutionContext

trait TerrainRenderer {
  def onTotalSizeChanged(totalSize: Int): Unit
  def onProjMatrixChanged(camera: Camera): Unit
  def regularChunkBufferFragmentation: IndexedSeq[Float]
  def transmissiveChunkBufferFragmentation: IndexedSeq[Float]
  def renderQueueLength: Int
  def render(camera: Camera, sun: Vector3f, opaque: Boolean, eyeUnderWater: Boolean): Unit

  /** Renders the opaque terrain from the point of view of a light, where `lightMatrix` replaces the view and
    * projection matrices of the camera
    */
  def renderShadowCasters(camera: Camera, lightMatrix: Matrix4f): Unit
  def tick(camera: Camera, renderDistance: Double, worldTickResult: WorldTickResult)(using ExecutionContext): Unit
  def unload(): Unit
}
