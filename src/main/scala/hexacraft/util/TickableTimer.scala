package hexacraft.util

class TickableTimer private (period: Int, delay: Int = 0, initActive: Boolean = true, action: =>Unit) {
  require(period >= 0)
  require(delay >= 0)

  private var time = delay
  var active: Boolean = initActive

  def tick(): Unit = {
    if (time > 0) time -= 1
    else if (active) {
      action
      time = period
    }
  }
}

object TickableTimer {
  def apply(period: Int, delay: Int = 0, initActive: Boolean = true)(action: =>Unit): TickableTimer =
    new TickableTimer(period, delay, initActive, action)
}
