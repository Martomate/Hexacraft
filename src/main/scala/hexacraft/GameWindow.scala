package hexacraft

import org.joml.{Vector2f, Vector2fc, Vector2i, Vector2ic}

trait GameWindow:
  def windowSize: Vector2ic
  def framebufferSize: Vector2ic

  def aspectRatio: Float = windowSize.x.toFloat / windowSize.y
