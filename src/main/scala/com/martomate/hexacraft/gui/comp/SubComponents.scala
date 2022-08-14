package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.gui.{CharEvent, KeyEvent, MouseClickEvent, ScrollEvent}

import scala.collection.mutable.ArrayBuffer

trait SubComponents extends Component {
  private val comps: ArrayBuffer[Component] = ArrayBuffer.empty

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    comps.exists(_.onMouseClickEvent(event)) || super.onMouseClickEvent(event)
  }

  override def onScrollEvent(event: ScrollEvent): Boolean = {
    comps.exists(_.onScrollEvent(event)) || super.onScrollEvent(event)
  }

  override def onKeyEvent(event: KeyEvent): Boolean = {
    comps.exists(_.onKeyEvent(event)) || super.onKeyEvent(event)
  }

  override def onCharEvent(event: CharEvent): Boolean = {
    comps.exists(_.onCharEvent(event)) || super.onCharEvent(event)
  }

  override def render(transformation: GUITransformation): Unit = {
    super.render(transformation)
    comps.foreach(_.render(transformation))
  }

  override def tick(): Unit = {
    super.tick()
    comps.foreach(_.tick())
  }

  def addComponent(comp: Component): Unit = {
    comps += comp
  }

  override def unload(): Unit = {
    comps.foreach(_.unload())
    super.unload()
  }
}
