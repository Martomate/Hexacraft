package hexacraft.util

import munit.FunSuite

class TickableTimerTest extends FunSuite {
  test("period must be positive") {
    intercept[IllegalArgumentException](TickableTimer(-1000))
    intercept[IllegalArgumentException](TickableTimer(-1))
    intercept[IllegalArgumentException](TickableTimer(0))

    // no exception should be thrown
    TickableTimer(1)
    TickableTimer(1000)
  }

  test("period should return true every 'period' ticks") {
    val timer = TickableTimer(3)

    for (i <- 0 until 10) {
      assertEquals(timer.tick(), i % 3 == 0)
    }
  }

  test("delay must be non-negative") {
    intercept[IllegalArgumentException](TickableTimer(1, -1000))
    intercept[IllegalArgumentException](TickableTimer(1, -1))

    // no exception should be thrown
    TickableTimer(1, 0)
    TickableTimer(1, 1000)
  }

  test("delay should delay the executions") {
    val timer = TickableTimer(3, 5)

    for (i <- 0 until 10) {
      assertEquals(timer.tick(), i >= 5 && (i - 5) % 3 == 0)
    }
  }

  test("enabled should be true by default") {
    assert(TickableTimer(1).enabled)
  }

  test("when enabled is false the timer should be disable") {
    val timer = TickableTimer(2, 0, false)
    assert(!timer.enabled)

    for (_ <- 0 until 10) assert(!timer.tick())
  }

  test("enabled should return true and reset the timer if 'period' ticks have passed when it turns on") {
    val timer = TickableTimer(3, 0, false)
    assert(!timer.enabled)

    // it's important for the test that 10 % 3 != 0, to test the reset
    for (_ <- 0 until 10) assert(!timer.tick())

    timer.enabled = true

    for (i <- 0 until 10) {
      assertEquals(timer.tick(), i % 3 == 0)
    }
  }
}
