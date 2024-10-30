package hexacraft.gui

import org.joml.{Vector2f, Vector2fc, Vector2ic}

case class RenderContext(
    windowAspectRatio: Float,
    frameBufferSize: Vector2ic,
    heightNormalizedMousePos: Vector2fc,
    offset: Vector2fc
) {
  def withMoreOffset(dx: Float, dy: Float): RenderContext = {
    this.copy(offset = offset.add(dx, dy, new Vector2f))
  }
}
