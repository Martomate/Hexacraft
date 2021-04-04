package com.martomate.hexacraft.world.coord.integer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OffsetTest extends AnyFlatSpec with Matchers {
  "+" should "work like addition" in {
    (Offset(2, 3, 4) + Offset(5, -4, 2)) shouldBe Offset(7, -1, 6)
  }

  "-" should "work like subtraction" in {
    (Offset(2, 3, 4) - Offset(5, -4, 2)) shouldBe Offset(-3, 7, 2)
  }

  "manhattanDistance" should "work for distance 0" in {
    Offset(0, 0, 0).manhattanDistance shouldBe 0
  }

  it should "work for distance 1" in {
    Offset(1, 0, 0).manhattanDistance shouldBe 1
    Offset(-1, 0, 0).manhattanDistance shouldBe 1
    Offset(0, 0, 1).manhattanDistance shouldBe 1
    Offset(0, 0, -1).manhattanDistance shouldBe 1
    Offset(1, 0, -1).manhattanDistance shouldBe 1
    Offset(-1, 0, 1).manhattanDistance shouldBe 1
    Offset(0, 1, 0).manhattanDistance shouldBe 1
    Offset(0, -1, 0).manhattanDistance shouldBe 1
  }

  it should "work for distance 2" in {
    Offset(2, 0, 0).manhattanDistance shouldBe 2
    Offset(1, 1, 0).manhattanDistance shouldBe 2
    Offset(0, 2, 0).manhattanDistance shouldBe 2
    Offset(0, 1, 1).manhattanDistance shouldBe 2
    Offset(0, 0, 2).manhattanDistance shouldBe 2
  }
}
