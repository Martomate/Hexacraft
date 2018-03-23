package hexacraft.util

import scala.collection.mutable

class RunOnToggle(onToOff: =>Unit, offToOn: =>Unit) {
  private var enabled: Boolean = false

  def activate(): Unit = {
    if (!enabled) {
      enabled = true
      offToOn
    }
  }

  def deactivate(): Unit = {
    if  (enabled) {
      enabled = false
      onToOff
    }
  }
}

class RunOnToggleWithIndex[T](indexing: T => Int)(onToOff: T => Unit, offToOn: T => Unit) {
  private val enabled: mutable.Set[Int] = new mutable.TreeSet()

  def activate(index: T): Unit = {
    val id = indexing(index)
    if (!enabled(id)) {
      enabled(id) = true
      offToOn(index)
    }
  }

  def deactivate(index: T): Unit = {
    val id = indexing(index)
    if  (enabled(id)) {
      enabled(id) = false
      onToOff(index)
    }
  }
}