package hexacraft.gui

import hexacraft.gui.comp.Component

import scala.collection.mutable.ArrayBuffer

abstract class Scene extends Component {
  private val onTickHandlers: ArrayBuffer[() => Unit] = ArrayBuffer.empty
  def onTick(handler: => Unit): Unit = onTickHandlers += (() => handler)

  override def tick(ctx: TickContext): Unit = {
    for handler <- onTickHandlers do {
      handler()
    }
  }

  def windowFocusChanged(focused: Boolean): Unit = ()
  def windowResized(w: Int, h: Int): Unit = ()
  def frameBufferResized(w: Int, h: Int): Unit = ()

  def isOpaque: Boolean = true

  override def handleEvent(event: Event): Boolean = true
}
