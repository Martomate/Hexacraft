package com.martomate.hexacraft.util

class TickableTimer private (period: Int, delay: Int, initActive: Boolean, action: =>Unit) {
  require(period > 0, "The period may not be negative")
  require(delay >= 0, "The delay may not be negative")

  private var time = delay
  var active: Boolean = initActive

  def tick(): Unit = {
    if (time > 0) time -= 1
    else if (active) {
      action
      time = period - 1
    }
  }
}

object TickableTimer {
  def apply(period: Int, delay: Int = 0, initActive: Boolean = true)(action: =>Unit): TickableTimer =
    new TickableTimer(period, delay, initActive, action)
}
