package hexacraft.util

import munit.FunSuite

import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

class TickLoopTest extends FunSuite {
  test("ticks repeatedly on another thread") {
    val latch = CountDownLatch(5)
    var tickThread: Thread = null

    val loop = TickLoop.start("test-loop", 100) { () =>
      tickThread = Thread.currentThread()
      latch.countDown()
    }
    try {
      assert(latch.await(1, TimeUnit.SECONDS), "the loop did not tick 5 times")
      assertNotEquals(tickThread, Thread.currentThread())
    } finally {
      loop.stop()
    }
  }

  test("does not tick after being stopped") {
    val ticks = AtomicInteger(0)

    val loop = TickLoop.start("test-loop", 100)(() => ticks.incrementAndGet())
    loop.stop()

    val ticksAfterStop = ticks.get()
    Thread.sleep(50)
    assertEquals(ticks.get(), ticksAfterStop)
  }

  test("stop waits for the current tick to finish") {
    val tickStarted = CountDownLatch(1)
    @volatile var tickFinished = false

    val loop = TickLoop.start("test-loop", 100) { () =>
      tickStarted.countDown()
      Thread.sleep(100)
      tickFinished = true
    }
    assert(tickStarted.await(1, TimeUnit.SECONDS), "the loop did not tick")
    assert(!tickFinished)
    loop.stop()
    assert(tickFinished)
  }

  test("stops ticking if a tick throws an exception") {
    val ticks = AtomicInteger(0)

    val loop = TickLoop.start("test-loop", 100) { () =>
      ticks.incrementAndGet()
      throw new RuntimeException("this exception is expected by the test")
    }
    try {
      Thread.sleep(100)
      assertEquals(ticks.get(), 1)
    } finally {
      loop.stop()
    }
  }
}
