package com.martomate.hexacraft.util

class TickableTimer private (period: Int, delay: Int, initActive: Boolean) {
  require(period > 0, "The period must be positive")
  require(delay >= 0, "The delay may not be negative")

  private var time = delay
  var active: Boolean = initActive

  def tick(): Boolean = {
    if (time > 0) {
      time -= 1
      false
    } else if (active) {
      time = period - 1
      true
    }else false
  }
}

object TickableTimer {
  def apply(period: Int, delay: Int = 0, initActive: Boolean = true): TickableTimer =
    new TickableTimer(period, delay, initActive)
}
