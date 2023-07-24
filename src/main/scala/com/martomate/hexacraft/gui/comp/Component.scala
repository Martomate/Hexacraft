package com.martomate.hexacraft.gui.comp

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.font.{Fonts, TextMaster}
import com.martomate.hexacraft.font.mesh.{Font, Text}
import com.martomate.hexacraft.gui.{Event, LocationInfo}
import com.martomate.hexacraft.infra.gpu.OpenGL
import com.martomate.hexacraft.renderer.{Blending, NoDepthTest, Renderer, TextureSingle, VAO}

import org.joml.{Matrix4f, Vector2f, Vector4f}

abstract class Component:
  private val textMaster = new TextMaster()

  protected def addText(text: Text): Unit = textMaster.loadText(text)
  protected def removeText(text: Text): Unit = textMaster.removeText(text)

  def tick(): Unit = ()

  def render(transformation: GUITransformation)(using window: GameWindow): Unit =
    textMaster.setWindowAspectRatio(window.aspectRatio)
    textMaster.render(transformation.x, transformation.y)

  def handleEvent(event: Event): Boolean = false
  def onReloadedResources(): Unit = ()

  def unload(): Unit = textMaster.unload()

object Component:
  private val rectVAO: VAO = VAO
    .builder()
    .addVertexVbo(4)(_.floats(0, 2), _.fillFloats(0, Seq(0, 0, 1, 0, 0, 1, 1, 1)))
    .finish(4)
  private val rectRenderer = new Renderer(rectVAO, OpenGL.PrimitiveMode.TriangleStrip) with NoDepthTest with Blending

  val font: Font = Fonts.get("Verdana").get

  private val imageShader = new ImageShader()
  private val colorShader = new ColorShader()

  def drawImage(
      location: LocationInfo,
      xoffset: Float,
      yoffset: Float,
      image: TextureSingle,
      windowAspectRatio: Float
  ): Unit =
    imageShader.enable()
    image.bind()

    imageShader.setTransformationMatrix(
      new Matrix4f()
        .translate(location.x + xoffset, location.y + yoffset, 0)
        .scale(location.w, location.h, 1)
    )

    imageShader.setWindowAspectRatio(windowAspectRatio)
    Component.rectRenderer.render()

  def drawRect(
      location: LocationInfo,
      xoffset: Float,
      yoffset: Float,
      color: Vector4f,
      windowAspectRatio: Float
  ): Unit =
    colorShader.enable()

    colorShader.setTransformationMatrix(
      new Matrix4f()
        .translate(location.x + xoffset, location.y + yoffset, 0)
        .scale(location.w, location.h, 1)
    )
    colorShader.setColor(color)

    colorShader.setWindowAspectRatio(windowAspectRatio)
    Component.rectRenderer.render()

  def makeText(text: String, location: LocationInfo, textSize: Float, centered: Boolean = true): Text =
    val position = new Vector2f(location.x, location.y + 0.5f * location.h + 0.015f * textSize)
    val guiText = Text(text, textSize, Component.font, position, location.w, centered)
    guiText.setColor(0.9f, 0.9f, 0.9f)
    guiText
