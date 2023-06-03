package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.font.{Fonts, TextMaster}
import com.martomate.hexacraft.font.mesh.{FontType, GUIText}
import com.martomate.hexacraft.gui.{Event, LocationInfo}
import com.martomate.hexacraft.renderer.*
import com.martomate.hexacraft.util.OpenGL

import org.joml.{Matrix4f, Vector2f, Vector4f}

abstract class Component:
  private val textMaster = new TextMaster()

  protected def addText(text: GUIText): Unit = textMaster.loadText(text)
  protected def removeText(text: GUIText): Unit = textMaster.removeText(text)

  def tick(): Unit = ()

  def render(transformation: GUITransformation)(using GameWindow): Unit =
    textMaster.render(transformation.x, transformation.y)

  def handleEvent(event: Event): Boolean = false
  def onReloadedResources(): Unit = ()

  def unload(): Unit = textMaster.unload()

object Component:
  private val rectVAO: VAO = VAO
    .builder()
    .addVBO(4)(_.floats(0, 2), _.fillFloats(0, Seq(0, 0, 1, 0, 0, 1, 1, 1)))
    .finish(4)
  private val rectRenderer = new Renderer(rectVAO, OpenGL.PrimitiveMode.TriangleStrip) with NoDepthTest with Blending

  val font: FontType = Fonts.get("Verdana").get

  private val imageShader: Shader = Shader.get(Shaders.ShaderNames.Image).get
  private val colorShader: Shader = Shader.get(Shaders.ShaderNames.Color).get

  def drawImage(location: LocationInfo, xoffset: Float, yoffset: Float, image: TextureSingle): Unit =
    imageShader.enable()
    image.bind()
    val mat = new Matrix4f()
      .translate(location.x + xoffset, location.y + yoffset, 0)
      .scale(location.w, location.h, 1)
    imageShader.setUniformMat4("transformationMatrix", mat)
    imageShader.setUniform2f("imageSize", image.width.toFloat, image.height.toFloat)
    Component.rectRenderer.render()

  def drawRect(location: LocationInfo, xoffset: Float, yoffset: Float, color: Vector4f): Unit =
    colorShader.enable()

    val mat = new Matrix4f()
      .translate(location.x + xoffset, location.y + yoffset, 0)
      .scale(location.w, location.h, 1)
    colorShader.setUniformMat4("transformationMatrix", mat)
    colorShader.setUniform4f("col", color)
    Component.rectRenderer.render()

  def makeText(text: String, location: LocationInfo, textSize: Float, centered: Boolean = true): GUIText =
    val position = new Vector2f(location.x, location.y + 0.5f * location.h + 0.015f * textSize)
    val guiText = GUIText(text, textSize, Component.font, position, location.w, centered)
    guiText.setColor(0.9f, 0.9f, 0.9f)
    guiText
