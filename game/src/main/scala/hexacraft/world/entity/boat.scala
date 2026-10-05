package hexacraft.world.entity

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.{BlockCoords, CylCoords}

import org.joml.{Vector3d, Vector3f}

class BoatEntityModel(
    body: BasicEntityPart,
    rods: Seq[BasicEntityPart],
    val textureName: String
) extends EntityModel {
  override val parts: Seq[EntityPart] = body +: rods

  override def tick(walking: Boolean, headDirection: Option[Vector3d]): Unit = {}
}

object BoatEntityModel {
  private def makeHexBox(r: Int, b: Float, h: Int): HexBox = {
    HexBox(r / 32f * 0.5f, b / 32f * 0.5f, (h + b) / 32f * 0.5f)
  }

  private def makePartPosition(xp: Double, yp: Double, zp: Double): BlockCoords.Offset = {
    BlockCoords.Offset(xp / 32.0, yp / 32.0, zp / 32.0)
  }

  def create(textureName: String): BoatEntityModel = {
    val rodLength = 64
    val rodRadius = 4

    val px = 2.0 / 3
    val pz = 1.0 / 3

    val rodBounds = makeHexBox(rodRadius, 0, rodLength)

    val elevation = 6f // a hack that ensures that no water is in the boat

    val rodPositions = Seq(
      (-3, 1),
      (-2, 0),
      (-1, 0),
      (0, 0),
      (1, 0),
      (2, 0),
      (2, 1)
    ).map { case (dx, dz) =>
      makePartPosition(
        -0.5 * rodLength * px,
        1.5 * rodRadius * dz,
        0.5 * rodLength * pz + rodRadius * dx + 0.5 * rodRadius * dz
      ).toCylCoordsOffset
    }

    val pi = math.Pi.toFloat

    val body =
      BasicEntityPart(HexBox(0, 0, 0), makePartPosition(0, elevation, 0).toCylCoordsOffset, Vector3f(0, pi / 2, 0))

    val rods = rodPositions.map { pos =>
      BasicEntityPart(rodBounds, pos, Vector3f(0, 0, -pi / 2), (0, 0), parentPart = body)
    }
    // TODO: add a cross rod in the front and back

    new BoatEntityModel(
      body = body,
      rods = rods,
      textureName = textureName
    )
  }
}
