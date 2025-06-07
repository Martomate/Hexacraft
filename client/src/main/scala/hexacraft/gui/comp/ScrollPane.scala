package hexacraft.gui.comp

import hexacraft.gui.{Event, LocationInfo, RenderContext, TickContext}
import hexacraft.infra.gpu.OpenGL
import hexacraft.math.MathUtils

import org.joml.Vector4f

import scala.collection.mutable.ArrayBuffer

class ScrollPane(
    location: LocationInfo,
    padding: Float = 0,
    enableHorizontalScroll: Boolean = false,
    enableVerticalScroll: Boolean = true,
    placeAtBottom: Boolean = false,
    transparentBackground: Boolean = false
) extends Component {
  private var xOffset: Float = 0
  private var yOffset: Float = 0

  private val components: ArrayBuffer[Component & Boundable] = ArrayBuffer.empty

  def addComponent(comp: Component & Boundable): Unit = {
    components.append(comp)
    clampScroll()
  }

  def scroll(dx: Float, dy: Float): Unit = {
    xOffset += dx
    yOffset += dy
    clampScroll()
  }

  def replaceComponent(idx: Int, comp: Component & Boundable): Unit = {
    require(idx >= 0 && idx <= components.length)

    if idx == components.length then {
      components.append(comp)
    } else if comp != components(idx) then {
      components(idx).unload()
      components(idx) = comp
    }
    clampScroll()
  }

  override def tick(ctx: TickContext): Unit = {
    super.tick(ctx)
    for c <- this.components do {
      c.tick(ctx)
    }
  }

  override def render(context: RenderContext): Unit = {
    if !transparentBackground then {
      Component.drawRect(
        location,
        context.offset.x,
        context.offset.y,
        new Vector4f(0, 0, 0, 0.4f),
        context.windowAspectRatio
      )
    }

    val contentTransformation = context.withMoreOffset(this.xOffset, this.yOffset)
    val loc = location.inScaledScreenCoordinates(context.frameBufferSize)

    OpenGL.glScissor(loc.x, loc.y, loc.w, loc.h)
    OpenGL.glEnable(OpenGL.State.ScissorTest)
    components.foreach(_.render(contentTransformation))
    OpenGL.glDisable(OpenGL.State.ScissorTest)

    super.render(contentTransformation)
  }

  override def handleEvent(event: Event): Boolean = event match {
    case event: Event.CharEvent => components.exists(_.handleEvent(event))
    case event: Event.KeyEvent  => components.exists(_.handleEvent(event))
    case event: Event.MouseClickEvent =>
      if containsMouse(event.mousePos) then {
        components.exists(_.handleEvent(event.withMouseTranslation(-xOffset, -yOffset)))
      } else {
        false
      }
    case Event.ScrollEvent(xOffset, yOffset, mousePos) =>
      if containsMouse(mousePos) then {
        if enableHorizontalScroll then {
          this.xOffset += xOffset * 0.05f
        }
        if enableVerticalScroll then {
          this.yOffset += -yOffset * 0.05f
        }

        clampScroll()
        true
      } else {
        false
      }
  }

  private def clampScroll(): Unit = {
    val boxBounds = location
    val contentBounds = calcContentBounds()

    if boxBounds.h - contentBounds.h < 2 * padding then {
      val limitTop = boxBounds.y + boxBounds.h - padding
      val limitBottom = boxBounds.y + padding
      val contentTop = contentBounds.y + contentBounds.h
      val contentBottom = contentBounds.y

      this.yOffset = MathUtils.clamp(this.yOffset, limitTop - contentTop, limitBottom - contentBottom)
    } else if placeAtBottom then {
      this.yOffset = boxBounds.y - contentBounds.y + padding
    } else {
      this.yOffset = boxBounds.y + boxBounds.h - (contentBounds.y + contentBounds.h) - padding
    }
  }

  private def containsMouse(mousePos: (Float, Float)): Boolean = {
    location.containsPoint(mousePos._1, mousePos._2)
  }

  override def unload(): Unit = {
    components.foreach(_.unload())
    super.unload()
  }

  private def calcContentBounds(): LocationInfo = {
    components
      .map(_.bounds)
      .reduceOption(LocationInfo.hull)
      .getOrElse(LocationInfo(0, 0, 0, 0))
  }
}
