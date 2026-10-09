package hexacraft.shaders

import hexacraft.renderer.Shader

import org.joml.{Vector3f, Vector3fc}

/** Shared settings for the fog that is drawn when looking through water */
object WaterFog {

  /** The color of water that is so deep that nothing behind it can be seen */
  val color: Vector3fc = new Vector3f(0.1f, 0.42f, 0.8f)

  /** How quickly each color channel of the light from an object is absorbed per unit of distance.
    * Red is absorbed first and blue last, like in real water, which keeps things blue instead of gray.
    */
  val absorption: Vector3fc = new Vector3f(0.2f, 0.06f, 0.02f)

  /** How quickly the color of the water itself takes over per unit of distance */
  val scattering: Float = 0.09f

  private[shaders] def setConstants(shader: Shader): Unit = {
    shader.setUniform3f("waterFogColor", color.x, color.y, color.z)
    shader.setUniform3f("waterAbsorption", absorption.x, absorption.y, absorption.z)
    shader.setUniform1f("waterScattering", scattering)
  }

  private[shaders] def setSurface(shader: Shader, surfaceAboveEye: Float, strength: Float): Unit = {
    shader.setUniform1f("waterSurfaceAboveEye", surfaceAboveEye)
    shader.setUniform1f("waterFogStrength", strength)
  }
}
