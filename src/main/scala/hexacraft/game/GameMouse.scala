package hexacraft.game

import org.joml.{Vector2f, Vector2fc, Vector2ic}

trait GameMouse:
  def pos: Vector2fc
  def moved: Vector2fc

  def normalizedScreenCoords(windowSize: Vector2ic): Vector2fc =
    val x = pos.x / windowSize.x * 2 - 1
    val y = pos.y / windowSize.y * 2 - 1
    new Vector2f(x, y)

  def heightNormalizedPos(windowSize: Vector2ic): Vector2fc =
    val aspectRatio = windowSize.x.toFloat / windowSize.y
    val x = (pos.x / windowSize.x * 2 - 1) * aspectRatio
    val y = pos.y / windowSize.y * 2 - 1
    new Vector2f(x, y)
