package hexacraft.util

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

object TickLoop {

  /** Calls `tick` at a fixed rate on a new thread until the loop is stopped. If a tick takes too long the following
    * ticks are run as soon as possible to catch up. If `tick` throws an exception the error is printed and the loop
    * stops ticking.
    */
  def start(name: String, ticksPerSecond: Int)(tick: () => Unit): TickLoop = {
    val executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory(name))
    val periodNanos = 1_000_000_000L / ticksPerSecond

    executor.scheduleAtFixedRate(
      () =>
        try {
          tick()
        } catch {
          case e: Throwable =>
            println(s"$name stopped ticking because of an error")
            e.printStackTrace()
            throw e // this prevents any further ticks
        },
      0,
      periodNanos,
      TimeUnit.NANOSECONDS
    )

    new TickLoop(executor)
  }
}

class TickLoop private (executor: ScheduledExecutorService) {

  /** Stops the loop and waits for the current tick (if any) to finish */
  def stop(): Unit = {
    executor.shutdown() // this cancels the future ticks, but does not interrupt the current tick
    executor.awaitTermination(10, TimeUnit.SECONDS)
  }
}
