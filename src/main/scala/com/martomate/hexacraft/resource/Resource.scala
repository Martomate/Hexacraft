package com.martomate.hexacraft.resource

import scala.collection.mutable.ArrayBuffer

object Resource {
  private val resources = ArrayBuffer.empty[Resource]

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
