package hexacraft

import org.joml.{Vector2dc, Vector2f, Vector2fc, Vector2ic}

trait GameMouse:
  def pos: Vector2dc
  def moved: Vector2dc

  def normalizedScreenCoords(windowSize: Vector2ic): Vector2fc =
    val x = (pos.x / windowSize.x * 2 - 1).toFloat
    val y = (pos.y / windowSize.y * 2 - 1).toFloat
    new Vector2f(x, y)

  def heightNormalizedPos(windowSize: Vector2ic): Vector2fc =
    val aspectRatio = windowSize.x.toFloat / windowSize.y
    val x = (pos.x / windowSize.x * 2 - 1).toFloat * aspectRatio
    val y = (pos.y / windowSize.y * 2 - 1).toFloat
    new Vector2f(x, y)
