package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.gui.Event

import scala.collection.mutable.ArrayBuffer

trait SubComponents extends Component:
  private val comps: ArrayBuffer[Component] = ArrayBuffer.empty

  override def handleEvent(event: Event): Boolean =
    comps.exists(_.handleEvent(event)) || super.handleEvent(event)

  override def render(transformation: GUITransformation): Unit =
    super.render(transformation)
    comps.foreach(_.render(transformation))

  override def tick(): Unit =
    super.tick()
    comps.foreach(_.tick())

  def addComponent(comp: Component): Unit =
    comps += comp

  override def unload(): Unit =
    comps.foreach(_.unload())
    super.unload()
