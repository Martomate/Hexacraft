package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{CharEvent, KeyEvent, LocationInfo, MouseClickEvent, ScrollEvent}
import com.martomate.hexacraft.util.MathUtils

import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import scala.collection.mutable.ArrayBuffer

class ScrollPane(
    location: LocationInfo,
    padding: Float = 0,
    enableHorizontalScroll: Boolean = false
)(using GameWindow)
    extends Component:
  private var xOffset: Float = 0
  private var yOffset: Float = 0

  private val components: ArrayBuffer[Component with Boundable] = ArrayBuffer.empty

  def addComponent(comp: Component with Boundable): Unit = components.append(comp)

  override def render(transformation: GUITransformation): Unit =
    Component.drawRect(location, transformation.x, transformation.y, new Vector4f(0, 0, 0, 0.4f))

    val contentTransformation = transformation.offset(this.xOffset, this.yOffset)
    val loc = location.inScaledScreenCoordinates
    GL11.glScissor(loc.x, loc.y, loc.w, loc.h)
    GL11.glEnable(GL11.GL_SCISSOR_TEST)
    components.foreach(_.render(contentTransformation))
    GL11.glDisable(GL11.GL_SCISSOR_TEST)
    super.render(contentTransformation)

  override def onScrollEvent(event: ScrollEvent): Boolean =
    if containsMouse
    then
      val boxBounds = location
      val contentBounds = calcContentBounds()

      if enableHorizontalScroll
      then this.xOffset += event.xoffset * 0.05f

      this.yOffset -= event.yoffset * 0.05f

      if boxBounds.h - contentBounds.h < 2 * padding
      then
        val limitTop = boxBounds.y + boxBounds.h - padding
        val limitBottom = boxBounds.y + padding
        val contentTop = contentBounds.y + contentBounds.h
        val contentBottom = contentBounds.y

        this.yOffset = MathUtils.clamp(yOffset, limitTop - contentTop, limitBottom - contentBottom)

      true
    else components.exists(_.onScrollEvent(event))

  override def onKeyEvent(event: KeyEvent): Boolean = components.exists(_.onKeyEvent(event))

  override def onCharEvent(event: CharEvent): Boolean = components.exists(_.onCharEvent(event))

  override def onMouseClickEvent(event: MouseClickEvent): Boolean =
    if containsMouse
    then components.exists(_.onMouseClickEvent(event.withMouseTranslation(-xOffset, -yOffset)))
    else false

  def containsMouse: Boolean = location.containsMouse(0, 0)

  override def unload(): Unit =
    components.foreach(_.unload())
    super.unload()

  def calcContentBounds(): LocationInfo =
    components
      .map(_.bounds)
      .reduceOption(LocationInfo.hull)
      .getOrElse(LocationInfo(0, 0, 0, 0))
