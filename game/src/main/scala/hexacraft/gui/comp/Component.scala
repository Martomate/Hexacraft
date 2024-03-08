package hexacraft.gui.comp

import hexacraft.gui.{Event, LocationInfo, RenderContext, TickContext}
import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.*
import hexacraft.shaders.{ColorShader, FancyRectShader, ImageShader}
import hexacraft.text.{Fonts, Text, TextMaster}
import hexacraft.text.font.Font
import org.joml.{Matrix4f, Vector2f, Vector4f}

abstract class Component {
  private val textMaster = new TextMaster()

  protected def addText(text: Text): Unit = textMaster.loadText(text)

  protected def removeText(text: Text): Unit = textMaster.removeText(text)

  def tick(ctx: TickContext): Unit = ()

  def render(transformation: GUITransformation)(using context: RenderContext): Unit = {
    textMaster.setWindowAspectRatio(context.windowAspectRatio)
    textMaster.render(transformation.x, transformation.y)
  }

  def handleEvent(event: Event): Boolean = false

  def unload(): Unit = {
    textMaster.unload()
  }
}

object Component {
  private val rectVAO: VAO = VAO
    .builder()
    .addVertexVbo(4)(_.floats(0, 2), _.fillFloats(0, Seq(0, 0, 1, 0, 0, 1, 1, 1)))
    .finish(4)
  private val rectRenderer = new Renderer(
    OpenGL.PrimitiveMode.TriangleStrip,
    GpuState.build(_.blend(true).depthTest(false))
  )

  val font: Font = Fonts.get("Verdana").get

  private val imageShader = new ImageShader()
  private val colorShader = new ColorShader()
  private val fancyRectShader = new FancyRectShader()

  def drawImage(
      location: LocationInfo,
      xOffset: Float,
      yOffset: Float,
      image: TextureSingle,
      windowAspectRatio: Float
  ): Unit = {
    imageShader.enable()
    image.bind()

    imageShader.setTransformationMatrix(
      new Matrix4f()
        .translate(location.x + xOffset, location.y + yOffset, 0)
        .scale(location.w, location.h, 1)
    )

    imageShader.setWindowAspectRatio(windowAspectRatio)
    Component.rectRenderer.render(rectVAO, rectVAO.maxCount)
  }

  def drawRect(
      location: LocationInfo,
      xoffset: Float,
      yoffset: Float,
      color: Vector4f,
      windowAspectRatio: Float
  ): Unit = {
    colorShader.enable()

    colorShader.setTransformationMatrix(
      new Matrix4f()
        .translate(location.x + xoffset, location.y + yoffset, 0)
        .scale(location.w, location.h, 1)
    )
    colorShader.setColor(color)

    colorShader.setWindowAspectRatio(windowAspectRatio)
    Component.rectRenderer.render(rectVAO, rectVAO.maxCount)
  }

  def drawFancyRect(
      location: LocationInfo,
      xoffset: Float,
      yoffset: Float,
      color: Vector4f,
      windowAspectRatio: Float,
      inverted: Boolean = false
  ): Unit = {
    fancyRectShader.enable()

    fancyRectShader.setTransformationMatrix(
      new Matrix4f()
        .translate(location.x + xoffset, location.y + yoffset, 0)
        .scale(location.w, location.h, 1)
    )
    fancyRectShader.setColor(color)
    fancyRectShader.setInverted(inverted)

    fancyRectShader.setWindowAspectRatio(windowAspectRatio)
    Component.rectRenderer.render(rectVAO, rectVAO.maxCount)
  }

  def makeText(
      text: String,
      location: LocationInfo,
      textSize: Float,
      centered: Boolean = true,
      shadow: Boolean = false
  ): Text = {
    val position = new Vector2f(location.x, location.y + 0.5f * location.h + 0.015f * textSize)
    val guiText = Text(text, textSize, Component.font, position, location.w, centered, shadow)

    if shadow then {
      guiText.setShadowColor(0.3f, 0.3f, 0.3f)
    }
    guiText.setColor(0.9f, 0.9f, 0.9f)

    guiText
  }
}
