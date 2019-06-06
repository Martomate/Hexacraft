package com.martomate.hexacraft.world.coord.fp

import com.martomate.hexacraft.util.CylinderSize
import org.scalatest.{FlatSpec, Matchers}

class CylCoordsTest extends FlatSpec with Matchers {
  implicit val cylSize: CylinderSize = new CylinderSize(3)
  val eps = 1e-9

  "distanceSq" should "be 0 for itself" in {
    val a = CylCoords(5,2,8)
    val b = CylCoords(5,2,8)
    a.distanceSq(b) shouldBe 0
  }
  it should "work for one axis without wrap" in {
    CylCoords(5,2,8) distanceSq CylCoords(5+2.3,2,8) shouldBe 2.3 * 2.3 +- eps
    CylCoords(5,2,8) distanceSq CylCoords(5-2.3,2,8) shouldBe 2.3 * 2.3 +- eps
    CylCoords(5,2,8) distanceSq CylCoords(5,2+2.3,8) shouldBe 2.3 * 2.3 +- eps
    CylCoords(5,2,8) distanceSq CylCoords(5,2-2.3,8) shouldBe 2.3 * 2.3 +- eps
    CylCoords(5,2,8) distanceSq CylCoords(5,2,8+2.3) shouldBe 2.3 * 2.3 +- eps
    CylCoords(5,2,8) distanceSq CylCoords(5,2,8-2.3) shouldBe 2.3 * 2.3 +- eps
  }
  it should "work for one axis with wrap" in {
    CylCoords(5,2,1-2.3) distanceSq CylCoords(5,2,1) shouldBe 2.3 * 2.3 +- eps
    CylCoords(5,2,1) distanceSq CylCoords(5,2,1-2.3) shouldBe 2.3 * 2.3 +- eps
  }
  it should "work for three axes without wrap" in {
    CylCoords(5,2,8) distanceSq CylCoords(7.3,2.1,8-2.9) shouldBe 2.3 * 2.3 + 0.1*0.1 + 2.9*2.9 +- eps
  }
  it should "work for three axes with wrap" in {
    CylCoords(5,2,1) distanceSq CylCoords(7.3,2.1,1-2.9) shouldBe 2.3 * 2.3 + 0.1*0.1 + 2.9*2.9 +- eps
  }
}
