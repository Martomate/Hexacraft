package hexacraft.client.entity

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.entity.{BasicEntityPart, EntityModel, EntityPart}

import org.joml.{Vector3d, Vector3f}

class BoatEntityModel(
    body: BasicEntityPart,
    rods: Seq[BasicEntityPart],
    val textureName: String
) extends EntityModel {
  override val parts: Seq[EntityPart] = body +: rods

  override def tick(walking: Boolean, headDirection: Option[Vector3d], sitting: Boolean): Unit = {}
}

object BoatEntityModel {
  import ModelUnits.*

  def create(textureName: String): BoatEntityModel = {
    val rodLength = 64
    val rodRadius = 4
    val crossRodLength = 8 * rodRadius * CylinderSize.y60.toFloat

    // Distances between the centers of neighbouring hexagonal rods
    val rodSpacing = hexStep(rodRadius) // within a row
    val rowHeight = 1.5 * rodRadius // between rows (each row is shifted half a rod sideways)

    val rodBounds = makeHexBox(rodRadius, 0, rodLength.toFloat)
    val crossRodBounds = makeHexBox(rodRadius, -0.5f * crossRodLength, crossRodLength)

    val elevation = 6 // a hack that ensures that no water is in the boat

    val rodPositions = Seq(
      (-3, 1),
      (-2, 0),
      (-1, 0),
      (0, 0),
      (1, 0),
      (2, 0),
      (2, 1)
    ).map { case (col, row) =>
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
    } ++ Seq(-0.2 * rodLength + rodRadius * 0.5, 0.8 * rodLength - rodRadius * 0.5).map { d =>
      val pos = cylOffset(0, 0, d)
      BasicEntityPart(crossRodBounds, pos, Vector3f(0, pi / 2, -pi / 2), (0, 0), parentPart = crossBody)
    }

    new BoatEntityModel(
      body = body,
      rods = rods,
      textureName = textureName
    )
  }
}
