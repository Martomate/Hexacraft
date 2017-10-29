package hexagon.gui.comp

import fontMeshCreator.{FontType, GUIText}
import hexagon.event.{KeyListener, MouseClickListener, MouseMoveListener}
import hexagon.font.{Fonts, TextMaster}
import hexagon.renderer._
import hexagon.resource.{Shader, TextureSingle}
import org.joml.{Matrix4f, Vector2f, Vector4f}
import org.lwjgl.opengl.GL11

abstract class Component(protected val location: LocationInfo) extends MouseMoveListener with MouseClickListener with KeyListener{
  private val textMaster = new TextMaster()

  protected def addText(text: GUIText): Unit = {
    textMaster.loadText(text)
  }

  protected def removeText(text: GUIText): Unit = {
    textMaster.removeText(text)
  }

  def render(): Unit = {
    textMaster.render()
  }

  def onMouseMoveEvent(x: Double, y: Double): Unit = Unit
  def onMouseClickEvent(button: Int, action: Int, mods: Int): Unit = Unit
  def onKeyEvent(key: Int, scancode: Int, action: Int, mods: Int): Unit = Unit

  def unload(): Unit = {
    textMaster.unload()
  }
}

object Component {
  private val rectVAO: VAO = new VAOBuilder(8).addVBO(VBO(4).floats(0, 2).create().fillFloats(0, Seq(0, 0, 1, 0, 0, 1, 1, 1))).create()
  private val rectRenderer = new Renderer(rectVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest with Blending

  val font: FontType = Fonts.get("Verdana").get

  private val imageShader: Shader = Shader.get("image").get
  private val colorShader: Shader = Shader.get("color").get

  def drawImage(location: LocationInfo, image: TextureSingle): Unit = {
    imageShader.enable()
    image.bind()
    val mat = new Matrix4f().translate(location.x, location.y, 0).scale(location.w, location.h, 1)
    imageShader.setUniformMat4("transformationMatrix", mat)
    Component.rectRenderer.render()
  }

  def drawRect(location: LocationInfo, color: Vector4f): Unit = {
    colorShader.enable()

    val mat = new Matrix4f().translate(location.x, location.y, 0).scale(location.w, location.h, 1)
    colorShader.setUniformMat4("transformationMatrix", mat)
    colorShader.setUniform4f("col", color)
    Component.rectRenderer.render()
  }

  def makeText(text: String, location: LocationInfo, textSize: Float): GUIText = {
    val guiText = new GUIText(text, textSize, Component.font, new Vector2f(location.x, location.y + 0.5f * location.h + 0.015f * textSize), location.w, true)
    guiText.setColour(0.9f, 0.9f, 0.9f)
    guiText
  }
}