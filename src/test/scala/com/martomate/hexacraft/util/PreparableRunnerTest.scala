package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PreparableRunnerTest extends AnyFlatSpec with Matchers {

  "onPrepare" should "be called when prepare is called" in {
    var prepFired: Boolean = false
    val runner = new PreparableRunner({ prepFired = true }, throw new RuntimeException("activation before preparation"))

    runner.prepare()
    prepFired shouldBe true
  }

  it should "only be called once between activations" in {
    var prepFired: Boolean = false
    val runner = new PreparableRunner({ prepFired = true }, throw new RuntimeException("activation before preparation"))

    runner.prepare()
    prepFired shouldBe true
    prepFired = false

    for (_ <- 1 to 4) {
      runner.prepare()
      prepFired shouldBe false
    }
  }

  it must "be called before activate for onActivate to run" in {
    var prepFired: Boolean = false
    var actFired: Boolean = false
    val runner = new PreparableRunner({ prepFired = true }, { actFired = true })

    runner.activate()
    actFired shouldBe false

    runner.prepare()
    prepFired shouldBe true

    runner.activate()
    actFired shouldBe true
  }

  "onActivate" should "only be called once per preparation" in {
    var actFired: Boolean = false
    val runner = new PreparableRunner((), { actFired = true })

    runner.prepare()

    runner.activate()
    actFired shouldBe true
    actFired = false

    for (_ <- 1 to 4) {
      runner.activate()
      actFired shouldBe false
    }
  }

  "the runner" should "be reusable" in {
    var prepFired: Boolean = false
    var activateFired: Boolean = false
    val runner = new PreparableRunner({ prepFired = true }, { activateFired = true })
    for (_ <- 1 to 4) {
      runner.prepare()
      assert(prepFired)
      prepFired = false

      runner.activate()
      assert(activateFired)
      activateFired = false
    }
  }

}
