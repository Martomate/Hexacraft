package hexacraft.server.entity

import hexacraft.world.CylinderSize
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import org.joml.Vector3f

object BoatEntityModel {
  import ModelUnits.*

  val model: EntityModel = create()

  private def create(): EntityModel = {
    val rodLength = 128
    val rodRadius = 4
    val bottomRodCount = 9 // should be odd so that the boat is symmetric
    val sideRowCount = 2 // the number of rows of rods above the bottom row

    // Distances between the centers of neighbouring hexagonal rods
    val rodSpacing = hexStep(rodRadius) // within a row
    val rowHeight = 1.5 * rodRadius // between rows (each row is shifted half a rod sideways)

    // In each row the cross rods reach from the center of the leftmost rod to the center of the rightmost one.
    // Every side row is half a rod wider on each side than the row below it. (rounded to whole pixels)
    def crossRodLength(row: Int): Int = math.round((bottomRodCount - 1 + row) * rodSpacing).toInt

    val rodPrism = HexPrism(rodRadius, rodLength)

    val elevation = 6 // a hack that ensures that no water is in the boat

    val halfBottomRodCount = bottomRodCount / 2
    val bottomRods = (-halfBottomRodCount to halfBottomRodCount).map(col => (col, 0))
    // Each side row sits half a rod further out than the row below it, so the sides lean outwards
    val sideRods = (1 to sideRowCount).flatMap(row => Seq((-halfBottomRodCount - row, row), (halfBottomRodCount, row)))

    val rodPositions = (bottomRods ++ sideRods).map { case (col, row) =>
      s"rod_${col}_$row" -> pos(
        -0.2 * rodLength, // centered along the rod's length
        rowHeight * row,
        rodSpacing * (col + 0.5 * row)
      )
    }

    val pi = math.Pi.toFloat

    // Pivots (not rendered)
    val body = EntityPart("body", HexPrism.empty, pos(0, elevation, 0), Vector3f(0, pi / 2, 0))
    val crossBody = EntityPart(
      "crossBody",
      HexPrism.empty,
      pos(0, 8 - 4 * CylinderSize.y60, 0),
      Vector3f(0, pi / 2, 0),
      Some("body")
    )

    val rods = rodPositions.map { (name, pos) =>
      EntityPart(name, rodPrism, pos, Vector3f(0, 0, -pi / 2), Some("body"))
    } ++ (0 until sideRowCount).flatMap { row =>
      val length = crossRodLength(row)
      val prism = HexPrism(rodRadius, length)

      Seq(
        "back" -> (-0.2 * rodLength + rodRadius * 0.5),
        "front" -> (0.8 * rodLength - rodRadius * 0.5)
      ).map { (end, d) =>
        val position = pos(0, rowHeight * row, d)
        val name = s"${end}CrossRod_$row"
        // the cross rods are centered around their attachment point
        EntityPart(name, prism, position, Vector3f(0, pi / 2, -pi / 2), Some("crossBody"), -0.5f * length)
      }
    }

    EntityModel((Seq(body, crossBody) ++ rods).toIndexedSeq)
  }
}
