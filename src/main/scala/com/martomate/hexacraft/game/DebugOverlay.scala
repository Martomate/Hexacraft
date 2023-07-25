package com.martomate.hexacraft.game

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.text.{Text, TextMaster}
import com.martomate.hexacraft.gui.LocationInfo
import com.martomate.hexacraft.gui.comp.{Component, GUITransformation}
import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import org.joml.{Vector3d, Vector3f}
import scala.collection.mutable

object DebugOverlay {
  case class Content(
      cameraPosition: CylCoords,
      cameraChunkCoords: ChunkRelWorld,
      cameraRotation: Vector3f,
      viewDistance: Double
  )

  object Content {
    def fromCamera(camera: Camera, viewDistance: Double)(using CylinderSize): Content =
      Content(CylCoords(camera.position), camera.blockCoords.getChunkRelWorld, camera.rotation, viewDistance)
  }
}

class DebugOverlay(initialAspectRatio: Float) {
  private val textDisplayMap = mutable.Map.empty[String, String]
  private val textValueMap = mutable.Map.empty[String, Text]
  private val texts = mutable.ArrayBuffer.empty[Text]

  private val textMaster = new TextMaster()

  private var yOff = 0.0f

  addLabel("Position")
  addDebugText("p.x", "x")
  addDebugText("p.y", "y")
  addDebugText("p.z", "z")

  addLabel("Chunk")
  addDebugText("c.x", "x")
  addDebugText("c.y", "y")
  addDebugText("c.z", "z")

  addLabel("Rotation")
  addDebugText("r.x", "x")
  addDebugText("r.y", "y")
  addDebugText("r.z", "z")

  addLabel("Other")
  addDebugText("viewDist", "viewDistance")

  alignTexts(initialAspectRatio)

  private def addLabel(text: String): Unit =
    yOff += 0.02f

    val location = LocationInfo.from16x9(0.01f, 0.95f - yOff, 0.2f, 0.05f)
    val guiText = Component.makeText(text, location, 2, centered = false)
    textMaster.loadText(guiText)
    texts += guiText

    yOff += 0.03f

  private def addDebugText(id: String, display: String): Unit =
    val location = LocationInfo.from16x9(0.01f, 0.95f - yOff, 0.2f, 0.05f)
    val guiText = Component.makeText("", location, 2, centered = false)
    textMaster.loadText(guiText)
    texts += guiText

    textDisplayMap += id -> display
    textValueMap += id -> guiText

    yOff += 0.03f

  private def setValue(name: String, value: Any): Unit =
    textValueMap(name).setText(textDisplayMap(name) + ": " + value)

  def updateContent(info: DebugOverlay.Content): Unit =
    setValue("p.x", info.cameraPosition.x.toFloat)
    setValue("p.y", info.cameraPosition.y.toFloat)
    setValue("p.z", info.cameraPosition.z.toFloat)

    setValue("c.x", info.cameraChunkCoords.X)
    setValue("c.y", info.cameraChunkCoords.Y)
    setValue("c.z", info.cameraChunkCoords.Z)

    setValue("r.x", info.cameraRotation.x)
    setValue("r.y", info.cameraRotation.y)
    setValue("r.z", info.cameraRotation.z)

    setValue("viewDist", f"${info.viewDistance}%.2f")

  def render(transformation: GUITransformation)(using window: GameWindow): Unit =
    textMaster.setWindowAspectRatio(window.aspectRatio)
    textMaster.render(transformation.x, transformation.y)

  def windowResized(w: Int, h: Int): Unit =
    alignTexts(w.toFloat / h)

  private def alignTexts(aspectRatio: Float): Unit =
    texts.foreach(t => t.setPosition(-aspectRatio + 0.01f * 2 * 16 / 9, t.position.y))

  def unload(): Unit = textMaster.unload()
}
