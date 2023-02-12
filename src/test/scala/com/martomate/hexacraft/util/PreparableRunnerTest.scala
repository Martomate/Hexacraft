package com.martomate.hexacraft.util

import munit.FunSuite

class PreparableRunnerTest extends FunSuite {

  test("onPrepare should be called when prepare is called") {
    var prepFired: Boolean = false
    val runner = new PreparableRunner({ prepFired = true }, throw new RuntimeException("activation before preparation"))

    runner.prepare()
    assert(prepFired)
  }

  test("onPrepare should only be called once between activations") {
    var prepFired: Boolean = false
    val runner = new PreparableRunner({ prepFired = true }, throw new RuntimeException("activation before preparation"))

    runner.prepare()
    assert(prepFired)
    prepFired = false

    for (_ <- 1 to 4) {
      runner.prepare()
      assert(!prepFired)
    }
  }

  test("onPrepare must be called before activate for onActivate to run") {
    var prepFired: Boolean = false
    var actFired: Boolean = false
    val runner = new PreparableRunner({ prepFired = true }, { actFired = true })

    runner.activate()
    assert(!actFired)

    runner.prepare()
    assert(prepFired)

    runner.activate()
    assert(actFired)
  }

  test("onActivate should only be called once per preparation") {
    var actFired: Boolean = false
    val runner = new PreparableRunner((), { actFired = true })

    runner.prepare()

    runner.activate()
    assert(actFired)
    actFired = false

    for (_ <- 1 to 4) {
      runner.activate()
      assert(!actFired)
    }
  }

  test("the runner should be reusable") {
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
