package com.martomate.hexacraft.util

import scala.collection.mutable

class PreparableRunnerWithIndex[T](indexing: T => Int)(
    onPrepare: T => Unit,
    onActivate: T => Unit
):
  private val prepared: mutable.Set[Int] = new mutable.TreeSet()

  def prepare(index: T): Unit =
    val id = indexing(index)
    if !prepared(id)
    then
      prepared(id) = true
      onPrepare(index)

  def activate(index: T): Unit =
    val id = indexing(index)
    if prepared(id)
    then
      prepared(id) = false
      onActivate(index)
