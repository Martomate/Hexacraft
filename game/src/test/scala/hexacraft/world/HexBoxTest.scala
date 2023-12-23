package hexacraft.world

import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords
import munit.FunSuite

class HexBoxTest extends FunSuite {
  given CylinderSize = CylinderSize(4)

  test("baseArea is correct") {
    val box = new HexBox(0.2f, 0.123f, 0.432f)
    assertEqualsDouble(box.baseArea, 0.2 * (0.2 * math.sqrt(3) / 2) / 2 * 6, 1e-6)
  }

  test("approximateVolumeOfIntersection is 0 if the distance is too high") {
    val box1 = new HexBox(0.3f, 0, 1)
    val box2 = new HexBox(0.2f, 0, 1)

    val pos1 = CylCoords(0, 0, 0)
    val pos2 = CylCoords(0.51, 0, 0)

    val dist = HexBox.approximateVolumeOfIntersection(pos1, box1, pos2, box2)

    assertEqualsDouble(dist, 0.0, 1e-6)
  }

  test("approximateVolumeOfIntersection is maximal if the distance is 0") {
    val box1 = new HexBox(0.3f, 0, 1)
    val box2 = new HexBox(0.2f, 0, 1)

    val pos1 = CylCoords(0, 0, 0)
    val pos2 = CylCoords(0, 0, 0)

    val dist = HexBox.approximateVolumeOfIntersection(pos1, box1, pos2, box2)

    assertEqualsDouble(dist, box2.baseArea * 1, 1e-6)
  }
}
