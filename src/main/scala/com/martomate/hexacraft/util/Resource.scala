package com.martomate.hexacraft.util

import scala.collection.mutable

object Resource {
  private val resources: mutable.Set[Resource] = mutable.Set.empty

  def freeAllResources(): Unit = for r <- resources.clone() do r.free()
  def reloadAllResources(): Unit = for r <- resources.clone() do r.reload()
}

abstract class Resource {
  private var hasBeenFreed = false

  Resource.resources += this

  final def free(): Unit =
    Resource.resources -= this
    if !hasBeenFreed then
      hasBeenFreed = true
      unload()

  protected def reload(): Unit
  protected def unload(): Unit
}
