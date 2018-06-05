package hexacraft.util

import scala.collection.mutable

class PreparableRunner(onPrepare: => Unit, onActivate: => Unit) {
  private var prepared: Boolean = false

  def prepare(): Unit = {
    if (!prepared) {
      prepared = true
      onPrepare
    }
  }

  def activate(): Unit = {
    if (prepared) {
      prepared = false
      onActivate
    }
  }
}

class PreparableRunnerWithIndex[T](indexing: T => Int)(onPrepare: T => Unit, onActivate: T => Unit) {
  private val prepared: mutable.Set[Int] = new mutable.TreeSet()

  def prepare(index: T): Unit = {
    val id = indexing(index)
    if (!prepared(id)) {
      prepared(id) = true
      onPrepare(index)
    }
  }

  def activate(index: T): Unit = {
    val id = indexing(index)
    if  (prepared(id)) {
      prepared(id) = false
      onActivate(index)
    }
  }
}