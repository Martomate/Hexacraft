package hexacraft.shaders

import hexacraft.renderer.Shader

import org.joml.{Vector3f, Vector3fc}

/** Shared settings for the fog that is drawn when looking through water */
object WaterFog {

  /** The color of water that is so deep that nothing behind it can be seen */
  val color: Vector3fc = new Vector3f(0.08f, 0.26f, 0.45f)

  /** How quickly each color channel is absorbed per unit of distance. Red is absorbed first, like in real water. */
  val absorption: Vector3fc = new Vector3f(0.16f, 0.075f, 0.05f)

  private[shaders] def setConstants(shader: Shader): Unit = {
    shader.setUniform3f("waterFogColor", color.x, color.y, color.z)
    shader.setUniform3f("waterAbsorption", absorption.x, absorption.y, absorption.z)
  }

  private[shaders] def setSurface(shader: Shader, surfaceAboveEye: Float, strength: Float): Unit = {
    shader.setUniform1f("waterSurfaceAboveEye", surfaceAboveEye)
    shader.setUniform1f("waterFogStrength", strength)
  }
}
