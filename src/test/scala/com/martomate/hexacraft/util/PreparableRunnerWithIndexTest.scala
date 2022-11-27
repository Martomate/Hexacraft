package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PreparableRunnerWithIndexTest extends AnyFlatSpec with Matchers {

  val indexFn: String => Int = s => s.toInt

  "onPrepare" should "be called when prepare is called" in {
    var prepFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(
      s => prepFired = Some(s),
      _ => throw new RuntimeException("activation before preparation")
    )

    runner.prepare("1337")
    prepFired shouldBe Some("1337")
  }

  it should "only be called once between activations" in {
    var prepFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(
      s => prepFired = Some(s),
      _ => throw new RuntimeException("activation before preparation")
    )

    runner.prepare("1337")
    prepFired shouldBe Some("1337")
    prepFired = None

    for (_ <- 1 to 4) {
      runner.prepare("1337")
      prepFired shouldBe None
    }
  }

  it must "be called before activate for onActivate to run" in {
    var prepFired: Option[String] = None
    var actFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), s => actFired = Some(s))

    runner.activate("1337")
    actFired shouldBe None

    runner.prepare("1337")
    prepFired shouldBe Some("1337")

    runner.activate("1337")
    actFired shouldBe Some("1337")
  }

  "onActivate" should "only be called once per preparation" in {
    var actFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(_ => (), s => actFired = Some(s))

    runner.prepare("1337")

    runner.activate("1337")
    actFired shouldBe Some("1337")
    actFired = None

    for (_ <- 1 to 4) {
      runner.activate("1337")
      actFired shouldBe None
    }
  }

  "it" should "be reusable" in {
    var prepFired: Option[String] = None
    var actFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), s => actFired = Some(s))
    for (_ <- 1 to 4) {
      runner.prepare("1337")
      prepFired shouldBe Some("1337")
      prepFired = None

      runner.activate("1337")
      actFired shouldBe Some("1337")
      actFired = None
    }
  }

  it should "handle different indices separately" in {
    var prepFired: Option[(String, Boolean)] = None
    val runner: PreparableRunnerWithIndex[String] =
      new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some((s, true)), s => prepFired = Some((s, false)))

    def testPrepare(idx: String): Unit = {
      runner.prepare(idx)
      prepFired shouldBe Some((idx, true))
      prepFired = None
    }

    def testActivate(idx: String): Unit = {
      runner.activate(idx)
      prepFired shouldBe Some((idx, false))
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
