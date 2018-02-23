package hexagon.resource

import scala.collection.mutable.Set
import scala.collection.mutable.ArrayBuffer

object Resource {
  private val resources = ArrayBuffer.empty[Resource]

  def freeAllResources(): Unit = {
    resources.foreach(_.unload())
    resources.clear()
  }
  
  def reloadAllResources(): Unit = {
    resources.foreach(_.reload())
  }
}

abstract class Resource {
  Resource.resources += this
  
  final def free(): Unit = {
    unload()
    Resource.resources -= this
  }
  
  protected def reload(): Unit
  protected def unload(): Unit
}
