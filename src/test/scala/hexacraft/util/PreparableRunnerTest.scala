package hexacraft.util

import org.scalatest.{FlatSpec, Matchers}

class PreparableRunnerTest extends FlatSpec with Matchers {

  "onPrepare" should "be called when prepare is called" in {
    var prepFired: Boolean = false
    val runner = new PreparableRunner({prepFired = true}, throw new RuntimeException("activation before preparation"))

    runner.prepare()
    prepFired shouldBe true
  }

  it should "only be called once between activations" in {
    var prepFired: Boolean = false
    val runner = new PreparableRunner({prepFired = true}, throw new RuntimeException("activation before preparation"))

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
    val runner = new PreparableRunner({prepFired = true}, {actFired = true})

    runner.activate()
    actFired shouldBe false

    runner.prepare()
    prepFired shouldBe true

    runner.activate()
    actFired shouldBe true
  }

  "onActivate" should "only be called once per preparation" in {
    var actFired: Boolean = false
    val runner = new PreparableRunner((), {actFired = true})

    runner.prepare()

    runner.activate()
    actFired shouldBe true
    actFired = false

    for (_ <- 1 to 4) {
      runner.activate()
      actFired shouldBe false
    }
  }

  "it" should "be reusable" in {
    var prepFired: Boolean = false
    var activateFired: Boolean = false
    val runner = new PreparableRunner({prepFired = true}, {activateFired = true})
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

class PreparableRunnerWithIndexTest extends FlatSpec with Matchers {

  val indexFn: String => Int = s => s.toInt

  "onPrepare" should "be called when prepare is called" in {
    var prepFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), _ => throw new RuntimeException("activation before preparation"))

    runner.prepare("1337")
    prepFired shouldBe Some("1337")
  }

  it should "only be called once between activations" in {
    var prepFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), _ => throw new RuntimeException("activation before preparation"))

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
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), s => actFired = Some(s))

    runner.activate("1337")
    actFired shouldBe None

    runner.prepare("1337")
    prepFired shouldBe Some("1337")

    runner.activate("1337")
    actFired shouldBe Some("1337")
  }

  "onActivate" should "only be called once per preparation" in {
    var actFired: Option[String] = None
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(_ => (), s => actFired = Some(s))

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
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some(s), s => actFired = Some(s))
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
    val runner: PreparableRunnerWithIndex[String] = new PreparableRunnerWithIndex(indexFn)(s => prepFired = Some((s, true)), s => prepFired = Some((s, false)))

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
