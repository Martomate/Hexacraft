package hexacraft.world.coord

import hexacraft.world.storage.CylinderSize
import org.scalatest.FunSuite

class ChunkRelColumnTest extends FunSuite {
  test("Y can use the entire range") {
    assertResult(   -14)(ChunkRelColumn(   -14, size).Y)
    assertResult( 0x7ff)(ChunkRelColumn( 0x7ff, size).Y)
    assertResult(-0x800)(ChunkRelColumn( 0x800, size).Y)
    assertResult(    -1)(ChunkRelColumn( 0xfff, size).Y)
    assertResult(     0)(ChunkRelColumn(0x1000, size).Y)
  }

  test("value is in YYY-format and correct") {
    assertResult(   -14 & 0xfff)(ChunkRelColumn(   -14, size).value)
    assertResult( 0x7ff)(ChunkRelColumn( 0x7ff, size).value)
    assertResult(-0x800 & 0xfff)(ChunkRelColumn( 0x800, size).value)
    assertResult(    -1 & 0xfff)(ChunkRelColumn( 0xfff, size).value)
    assertResult(     0)(ChunkRelColumn(0x1000, size).value)
  }

  private def size = new CylinderSize(4)
}
