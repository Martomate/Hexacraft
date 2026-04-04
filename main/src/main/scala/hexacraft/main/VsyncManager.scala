package hexacraft.main

class VsyncManager(lo: Int, hi: Int, value: => Boolean, setValue: Boolean => Unit) {
  private var consecutiveToggleAttempts = 0

  def handleVsync(fps: Int): Unit = {
    val newValue = shouldUseVsync(fps)

    if newValue != value then {
      consecutiveToggleAttempts += 1
    } else {
      consecutiveToggleAttempts = 0
    }

    if consecutiveToggleAttempts >= 3
    then {
      consecutiveToggleAttempts = 0
      setValue(newValue)
    }
  }

  private def shouldUseVsync(fps: Int): Boolean = {
    if fps > hi then {
      true
    } else if fps < lo then {
      false
    } else {
      value
    }
  }
}
