package hexacraft.gui

import org.joml.{Vector2fc, Vector2ic}

case class RenderContext(
    windowAspectRatio: Float,
    frameBufferSize: Vector2ic,
    heightNormalizedMousePos: Vector2fc
)
