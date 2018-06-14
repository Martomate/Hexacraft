package hexacraft.world.coord

import hexacraft.world.storage.CylinderSize
import org.scalatest.FunSuite

class ChunkRelWorldTest extends FunSuite {
  test("Y is correct in entire range") {
    assertResult(   -14)(ChunkRelWorld(532,    -14, 17, size).Y)
    assertResult( 0x7ff)(ChunkRelWorld(532,  0x7ff, 17, size).Y)
    assertResult(-0x800)(ChunkRelWorld(532,  0x800, 17, size).Y)
    assertResult(    -1)(ChunkRelWorld(532,  0xfff, 17, size).Y)
    assertResult(     0)(ChunkRelWorld(532, 0x1000, 17, size).Y)
  }

  test("X is correct in entire range") {
    assertResult(     -14)(ChunkRelWorld(     -14, -14, 17, size).X)
    assertResult( 0x7ffff)(ChunkRelWorld( 0x7ffff, -14, 17, size).X)
    assertResult(-0x80000)(ChunkRelWorld( 0x80000, -14, 17, size).X)
    assertResult(      -1)(ChunkRelWorld( 0xfffff, -14, 17, size).X)
    assertResult(       0)(ChunkRelWorld(0x100000, -14, 17, size).X)
  }

  test("Z is correct in entire range") {
    assertResult( 1)(ChunkRelWorld(532, -14,  1, size).Z)
    assertResult(15)(ChunkRelWorld(532, -14, -1, size).Z)
    assertResult( 0)(ChunkRelWorld(532, -14, 16, size).Z)
    assertResult( 0)(ChunkRelWorld(532, -14, 16 * 25235, size).Z)
  }

  test("offset works") {
    val c = ChunkRelWorld(532, -14, 17, size).offset(-4, 71, -12345)
    assertResult(532 -  4)(c.X)
    assertResult(-14 + 71)(c.Y)
    assertResult(       8)(c.Z)
  }

  test("value is in XXXXXZZZZZYYY-format and correct") {
    assertResult(532L << 32 | (17L & 15) << 12 | (-14 & 0xfff))(ChunkRelWorld(532, -14, 17, size).value)
    assertResult((-532L & 0xfffff) << 32 | (17L & 15) << 12 | (-14 & 0xfff))(ChunkRelWorld(-532, -14, 17, size).value)
    assertResult(532L << 32 | (-17L & 15) << 12 | (-14 & 0xfff))(ChunkRelWorld(532, -14, -17, size).value)
  }

  test("withBlockCoords works") {
    var c = ChunkRelWorld(532, -14, 17, size).withBlockCoords(0, 0, 0)
    assertResult(532 * 16)(c.x)
    assertResult(-14 * 16)(c.y)
    assertResult(  1 * 16)(c.z)

    c = ChunkRelWorld(532, -14, 17, size).withBlockCoords(-4, 71, -12345)
    assertResult(532 * 16 -  4)(c.x)
    assertResult(-14 * 16 + 71)(c.y)
    assertResult( (17 * 16 - 12345) & size.totalSizeMask)(c.z)
  }

  private def size = new CylinderSize(4)
}
