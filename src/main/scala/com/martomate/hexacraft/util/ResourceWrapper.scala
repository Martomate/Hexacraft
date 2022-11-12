package com.martomate.hexacraft.util

class ResourceWrapper[T](make: => T) extends Resource {
  private var elem: T = _
  def get: T = elem

  reload()

  def reload(): Unit = {
    elem = make
  }

  def unload(): Unit = {}
}
