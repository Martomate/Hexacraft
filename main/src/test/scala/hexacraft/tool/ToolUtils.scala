package hexacraft.tool

object ToolUtils {

  def runAtSteadyFps(fps: Int)(running: => Boolean)(tick: => Unit): Unit = {
    while running do {
      val before = System.currentTimeMillis()
      tick
      val after = System.currentTimeMillis()
      val sleepTime = before + 1000 / fps - after
      if sleepTime > 0 then {
        Thread.sleep(sleepTime)
      }
    }
  }
}
