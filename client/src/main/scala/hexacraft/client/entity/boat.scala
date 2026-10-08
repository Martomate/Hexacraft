package hexacraft.client.entity

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.entity.{BasicEntityPart, EntityModel, EntityPart}

import org.joml.{Vector3d, Vector3dc, Vector3f}

class BoatEntityModel(
    body: BasicEntityPart,
    rods: Seq[BasicEntityPart],
    val textureName: String
) extends EntityModel {
  override val parts: Seq[EntityPart] = body +: rods

  override def tick(
      walking: Boolean,
      headDirection: Option[Vector3d],
      rotation: Vector3dc,
      mountRotation: Option[Vector3dc]
  ): Unit = {}
}

object BoatEntityModel {
  import ModelUnits.*

  def create(textureName: String): BoatEntityModel = {
    val rodLength = 128
    val rodRadius = 4
    val bottomRodCount = 9 // should be odd so that the boat is symmetric
    val sideRowCount = 2 // the number of rows of rods above the bottom row

    // Distances between the centers of neighbouring hexagonal rods
    val rodSpacing = hexStep(rodRadius) // within a row
    val rowHeight = 1.5 * rodRadius // between rows (each row is shifted half a rod sideways)

    // In each row the cross rods reach from the center of the leftmost rod to the center of the rightmost one.
    // Every side row is half a rod wider on each side than the row below it.
    def crossRodLength(row: Int): Float = ((bottomRodCount - 1 + row) * rodSpacing).toFloat

    val rodBounds = makeHexBox(rodRadius, 0, rodLength.toFloat)

    val elevation = 6 // a hack that ensures that no water is in the boat

    val halfBottomRodCount = bottomRodCount / 2
    val bottomRods = (-halfBottomRodCount to halfBottomRodCount).map(col => (col, 0))
    // Each side row sits half a rod further out than the row below it, so the sides lean outwards
    val sideRods = (1 to sideRowCount).flatMap(row => Seq((-halfBottomRodCount - row, row), (halfBottomRodCount, row)))

    val rodPositions = (bottomRods ++ sideRods).map { case (col, row) =>
      cylOffset(
        -0.2 * rodLength, // centered along the rod's length
        rowHeight * row,
        rodSpacing * (col + 0.5 * row)
      )
    }

    val pi = math.Pi.toFloat

    val body = BasicEntityPart(
      HexBox(0, 0, 0),
      cylOffset(0, elevation, 0),
      Vector3f(0, pi / 2, 0)
    )
    val crossBody = BasicEntityPart(
      HexBox(0, 0, 0),
      cylOffset(0, 8 - 4 * CylinderSize.y60, 0),
      Vector3f(0, pi / 2, 0),
      parentPart = body
    )

    val rods = rodPositions.map { pos =>
      BasicEntityPart(rodBounds, pos, Vector3f(0, 0, -pi / 2), (0, 0), parentPart = body)
    } ++ (0 until sideRowCount).flatMap { row =>
      val length = crossRodLength(row)
      val bounds = makeHexBox(rodRadius, -0.5f * length, length)

      Seq(-0.2 * rodLength + rodRadius * 0.5, 0.8 * rodLength - rodRadius * 0.5).map { d =>
        val pos = cylOffset(0, rowHeight * row, d)
        BasicEntityPart(bounds, pos, Vector3f(0, pi / 2, -pi / 2), (0, 0), parentPart = crossBody)
      }
    }

    new BoatEntityModel(
      body = body,
      rods = rods,
      textureName = textureName
    )
  }
}
