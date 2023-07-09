package com.martomate.hexacraft.util

class TickableTimer private (period: Int, delay: Int, initEnabled: Boolean):
  require(period > 0, "The period must be positive")
  require(delay >= 0, "The delay may not be negative")

  private var time = delay
  var enabled: Boolean = initEnabled

  def tick(): Boolean =
    if time > 0
    then
      time -= 1
      false
    else if enabled
    then
      time = period - 1
      true
    else false

object TickableTimer:
  def apply(period: Int, delay: Int = 0, initEnabled: Boolean = true): TickableTimer =
    new TickableTimer(period, delay, initEnabled)
