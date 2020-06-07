package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TickableTimerTest extends AnyFlatSpec with Matchers {
  "period" must "be positive" in {
    an [IllegalArgumentException] should be thrownBy TickableTimer(-1000)
    an [IllegalArgumentException] should be thrownBy TickableTimer(-1)
    an [IllegalArgumentException] should be thrownBy TickableTimer(0)
    noException should be thrownBy TickableTimer(1)
    noException should be thrownBy TickableTimer(1000)
  }

  it should "return true every 'period' ticks" in {
    val timer = TickableTimer(3)

    for (i <- 0 until 10) {
      timer.tick() shouldBe (i % 3 == 0)
    }
  }

  "delay" must "be non-negative" in {
    an [IllegalArgumentException] should be thrownBy TickableTimer(1, -1000)
    an [IllegalArgumentException] should be thrownBy TickableTimer(1, -1)
    noException should be thrownBy TickableTimer(1, 0)
    noException should be thrownBy TickableTimer(1, 1000)
  }

  it should "delay the executions" in {
    val timer = TickableTimer(3, 5)

    for (i <- 0 until 10) {
      timer.tick() shouldBe i >= 5 && (i - 5) % 3 == 0
    }
  }

  "active" should "be true by default" in {
    TickableTimer(1).active shouldBe true
  }

  it should "disable the timer when false" in {
    val timer = TickableTimer(2, 0, false)
    timer.active shouldBe false

    for (_ <- 0 until 10) timer.tick() shouldBe false
  }

  it should "return true and reset the timer if 'period' ticks have passed when it turns on" in {
    val timer = TickableTimer(3, 0, false)
    timer.active shouldBe false

    // it's important for the test that 10 % 3 != 0, to test the reset
    for (_ <- 0 until 10) timer.tick() shouldBe false

    timer.active = true

    for (i <- 0 until 10) {
      timer.tick() shouldBe i % 3 == 0
    }
  }
}
