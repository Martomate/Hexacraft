package hexacraft.gui

import org.joml.Vector2ic

case class WindowSize(logicalSize: Vector2ic, physicalSize: Vector2ic) {
  def logicalAspectRatio: Float = logicalSize.x.toFloat / logicalSize.y

  def physicalAspectRatio: Float = physicalSize.x.toFloat / physicalSize.y
}
