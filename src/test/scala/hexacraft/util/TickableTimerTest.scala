package hexacraft.util

import org.scalatest.{FlatSpec, Matchers}

class TickableTimerTest extends FlatSpec with Matchers {
  "period" must "be positive" in {
    an [IllegalArgumentException] should be thrownBy TickableTimer(-1000)(Unit)
    an [IllegalArgumentException] should be thrownBy TickableTimer(-1)(Unit)
    an [IllegalArgumentException] should be thrownBy TickableTimer(0)(Unit)
    noException should be thrownBy TickableTimer(1)(Unit)
    noException should be thrownBy TickableTimer(1000)(Unit)
  }

  it should "make the action run every 'period' ticks" in {
    var ran = false
    val timer = TickableTimer(3){ran = true}

    for (i <- 0 until 10) {
      timer.tick()
      ran shouldBe i % 3 == 0
      ran = false
    }
  }

  "delay" must "be non-negative" in {
    an [IllegalArgumentException] should be thrownBy TickableTimer(1, -1000)(Unit)
    an [IllegalArgumentException] should be thrownBy TickableTimer(1, -1)(Unit)
    noException should be thrownBy TickableTimer(1, 0)(Unit)
    noException should be thrownBy TickableTimer(1, 1000)(Unit)
  }

  it should "delay the executions" in {
    var ran = false
    val timer = TickableTimer(3, 5){ran = true}

    for (i <- 0 until 10) {
      timer.tick()
      ran shouldBe i >= 5 && (i - 5) % 3 == 0
      ran = false
    }
  }

  "active" should "be true by default" in {
    TickableTimer(1)(Unit).active shouldBe true
  }

  it should "disable the timer when false" in {
    val timer = TickableTimer(2, 0, false)(fail)
    timer.active shouldBe false

    for (_ <- 0 until 10) timer.tick()
  }

  it should "set off the action and reset the timer if 'period' ticks have passed when it turns on" in {
    var ran = false
    val timer = TickableTimer(3, 0, false){ran = true}
    timer.active shouldBe false

    // it's important for the test that 10 % 3 != 0, to test the reset
    for (_ <- 0 until 10) timer.tick()

    ran shouldBe false
    timer.active = true

    for (i <- 0 until 10) {
      timer.tick()
      ran shouldBe i % 3 == 0
      ran = false
    }
  }
}
