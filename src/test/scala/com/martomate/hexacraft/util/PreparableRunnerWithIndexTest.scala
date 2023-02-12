package com.martomate.hexacraft.util

import munit.FunSuite

class PreparableRunnerWithIndexTest extends FunSuite {

  val indexFn: String => Int = s => s.toInt

  test("onPrepare should be called when prepare is called") {
    var prepFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(
      s => prepFired = Some(s),
      _ => throw new RuntimeException("activation before preparation")
    )

    runner.prepare("1337")
    assertEquals(prepFired, Some("1337"))
  }

  test("onPrepare should only be called once between activations") {
    var prepFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(
      s => prepFired = Some(s),
      _ => throw new RuntimeException("activation before preparation")
    )

    runner.prepare("1337")
    assertEquals(prepFired, Some("1337"))
    prepFired = None

    for (_ <- 1 to 4) {
      runner.prepare("1337")
      assertEquals(prepFired, None)
    }
  }

  test("onPrepare must be called before activate for onActivate to run") {
    var prepFired: Option[String] = None
    var actFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), s => actFired = Some(s))

    runner.activate("1337")
    assertEquals(actFired, None)

    runner.prepare("1337")
    assertEquals(prepFired, Some("1337"))

    runner.activate("1337")
    assertEquals(actFired, Some("1337"))
  }

  test("onActivate should only be called once per preparation") {
    var actFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(_ => (), s => actFired = Some(s))

    runner.prepare("1337")

    runner.activate("1337")
    assertEquals(actFired, Some("1337"))
    actFired = None

    for (_ <- 1 to 4) {
      runner.activate("1337")
      assertEquals(actFired, None)
    }
  }

  test("onActivate should be reusable") {
    var prepFired: Option[String] = None
    var actFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), s => actFired = Some(s))
    for (_ <- 1 to 4) {
      runner.prepare("1337")
      assertEquals(prepFired, Some("1337"))
      prepFired = None

      runner.activate("1337")
      assertEquals(actFired, Some("1337"))
      actFired = None
    }
  }

  test("onActivate should handle different indices separately") {
    var prepFired: Option[(String, Boolean)] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some((s, true)), s => prepFired = Some((s, false)))

    def testPrepare(idx: String): Unit = {
      runner.prepare(idx)
      assertEquals(prepFired, Some((idx, true)))
      prepFired = None
    }

    def testActivate(idx: String): Unit = {
      runner.activate(idx)
      assertEquals(prepFired, Some((idx, false)))
      prepFired = None
    }

    testPrepare("1337")
    testActivate("1337")

    testPrepare("97")

    testPrepare("1337")
    testPrepare("42")
    testActivate("42")
    testActivate("1337")

    testPrepare("42")
    testPrepare("1337")
    testActivate("42")
    testActivate("1337")

    testActivate("97")
  }

}
