package com.martomate.hexacraft.util

import scala.collection.mutable

object Resource {
  private val resources: mutable.Set[Resource] = mutable.Set.empty

  def freeAllResources(): Unit = {
    resources.clone().foreach(_.free1())
    resources.clear()
  }

  def reloadAllResources(): Unit = {
    resources.foreach(_.reload())
  }
}

abstract class Resource {
  private var hasBeenFreed = false

  Resource.resources += this

  final def free(): Unit = {
    Resource.resources -= this
    free1()
  }

  private def free1(): Unit = {
    if (!hasBeenFreed) {
      hasBeenFreed = true
      unload()
    }
  }

  protected def reload(): Unit
  protected def unload(): Unit
}
