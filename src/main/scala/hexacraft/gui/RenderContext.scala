package hexacraft.gui

import org.joml.{Vector2fc, Vector2ic}

case class RenderContext(
    windowAspectRatio: Float,
    framebufferSize: Vector2ic,
    heightNormalizedMousePos: Vector2fc
)
