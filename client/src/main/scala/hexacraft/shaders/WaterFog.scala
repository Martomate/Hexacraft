package hexacraft.shaders

import hexacraft.renderer.Shader

import org.joml.{Vector3f, Vector3fc}

/** Shared settings for the fog that is drawn when looking through water */
object WaterFog {

  /** The color of the water itself (seen when nothing is behind it) right below the surface. It is darker deeper down. */
  val color: Vector3fc = new Vector3f(0.04f, 0.2f, 0.4f)

  /** How quickly each color channel of the light from an object is absorbed per unit of distance.
    * Red is absorbed first and blue last, like in real water, which keeps things blue instead of gray.
    */
  val absorption: Vector3fc = new Vector3f(0.2f, 0.06f, 0.02f)

  /** How quickly the color of the water itself takes over per unit of distance */
  val scattering: Float = 0.09f

  /** How quickly the light gets darker per unit of depth below the surface (in addition to `absorption`) */
  val depthDarkening: Float = 0.04f

  private[shaders] def setConstants(shader: Shader): Unit = {
    shader.setUniform3f("waterFogColor", color.x, color.y, color.z)
    shader.setUniform3f("waterAbsorption", absorption.x, absorption.y, absorption.z)
    shader.setUniform1f("waterScattering", scattering)
    shader.setUniform1f("waterDepthDarkening", depthDarkening)
  }

  private[shaders] def setSurface(shader: Shader, surfaceAboveEye: Float, strength: Float): Unit = {
    shader.setUniform1f("waterSurfaceAboveEye", surfaceAboveEye)
    shader.setUniform1f("waterFogStrength", strength)
  }
}
