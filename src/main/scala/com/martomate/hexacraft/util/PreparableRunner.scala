package com.martomate.hexacraft.util

class PreparableRunner(onPrepare: => Unit, onActivate: => Unit):
  private var prepared: Boolean = false

  def prepare(): Unit =
    if !prepared
    then
      prepared = true
      onPrepare

  def activate(): Unit =
    if prepared
    then
      prepared = false
      onActivate
