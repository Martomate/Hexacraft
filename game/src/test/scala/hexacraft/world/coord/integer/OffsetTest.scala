package hexacraft.world.coord.integer

import hexacraft.world.coord.Offset
import munit.FunSuite

class OffsetTest extends FunSuite {
  test("+ should work like addition") {
    assertEquals(Offset(2, 3, 4) + Offset(5, -4, 2), Offset(7, -1, 6))
  }

  test("- should work like subtraction") {
    assertEquals(Offset(2, 3, 4) - Offset(5, -4, 2), Offset(-3, 7, 2))
  }

  test("manhattanDistance should work for distance 0") {
    assertEquals(Offset(0, 0, 0).manhattanDistance, 0)
  }

  test("manhattanDistance should work for distance 1") {
    assertEquals(Offset(1, 0, 0).manhattanDistance, 1)
    assertEquals(Offset(-1, 0, 0).manhattanDistance, 1)
    assertEquals(Offset(0, 0, 1).manhattanDistance, 1)
    assertEquals(Offset(0, 0, -1).manhattanDistance, 1)
    assertEquals(Offset(1, 0, -1).manhattanDistance, 1)
    assertEquals(Offset(-1, 0, 1).manhattanDistance, 1)
    assertEquals(Offset(0, 1, 0).manhattanDistance, 1)
    assertEquals(Offset(0, -1, 0).manhattanDistance, 1)
  }

  test("manhattanDistance should work for distance 2") {
    assertEquals(Offset(2, 0, 0).manhattanDistance, 2)
    assertEquals(Offset(1, 1, 0).manhattanDistance, 2)
    assertEquals(Offset(0, 2, 0).manhattanDistance, 2)
    assertEquals(Offset(0, 1, 1).manhattanDistance, 2)
    assertEquals(Offset(0, 0, 2).manhattanDistance, 2)
  }
}
