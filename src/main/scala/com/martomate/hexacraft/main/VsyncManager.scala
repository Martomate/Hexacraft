package com.martomate.hexacraft.main

class VsyncManager(lo: Int, hi: Int, onUpdate: Boolean => Unit):
  private var vsync = true
  private var consecutiveToggleAttempts = 0

  def isVsync: Boolean = vsync

  def handleVsync(fps: Int): Unit =
    val newVsync = shouldUseVsync(fps)

    if newVsync != vsync
    then consecutiveToggleAttempts += 1
    else consecutiveToggleAttempts = 0

    if consecutiveToggleAttempts >= 3
    then
      consecutiveToggleAttempts = 0
      vsync = newVsync
      onUpdate(vsync)

  private def shouldUseVsync(fps: Int): Boolean =
    if fps > hi
    then true
    else if fps < lo
    then false
    else vsync
