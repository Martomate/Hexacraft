package com.martomate.hexacraft.game

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.font.mesh.GUIText
import com.martomate.hexacraft.gui.{LocationInfo, Scene}
import com.martomate.hexacraft.gui.comp.Component
import com.martomate.hexacraft.world.DebugInfoProvider

import scala.collection.mutable

class DebugScene(info: DebugInfoProvider)(implicit window: GameWindow) extends Scene {
  private val textDisplayMap = mutable.Map.empty[String, String]
  private val textValueMap = mutable.Map.empty[String, GUIText]
  private val texts = mutable.ArrayBuffer.empty[GUIText]

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

  alignTexts(window.aspectRatio)

  override def isOpaque: Boolean = false

  private def addLabel(text: String): Unit = {
    yOff += 0.02f
    val guiText = Component.makeText(
      text,
      LocationInfo.from16x9(0.01f, 0.95f - yOff, 0.2f, 0.05f),
      2,
      centered = false
    )
    addText(guiText)
    yOff += 0.03f
    texts += guiText
  }

  private def addDebugText(id: String, display: String, defaultValue: String = ""): Unit = {
    val text = Component.makeText(
      defaultValue,
      LocationInfo.from16x9(0.01f, 0.95f - yOff, 0.2f, 0.05f),
      2,
      centered = false
    )
    addText(text)
    yOff += 0.03f
    textDisplayMap += id -> display
    textValueMap += id -> text
    texts += text
  }

  private def setValue(name: String, value: Any): Unit = {
    textValueMap(name).setText(textDisplayMap(name) + ": " + value)
  }

  override def tick(): Unit = {
    setValue("p.x", info.camera.position.x.toFloat)
    setValue("p.y", info.camera.position.y.toFloat)
    setValue("p.z", info.camera.position.z.toFloat)

    setValue("c.x", info.camera.blockCoords.x >> 4)
    setValue("c.y", info.camera.blockCoords.y >> 4)
    setValue("c.z", info.camera.blockCoords.z >> 4)

    setValue("r.x", info.camera.rotation.x)
    setValue("r.y", info.camera.rotation.y)
    setValue("r.z", info.camera.rotation.z)

    setValue("viewDist", (100 * info.viewDistance).toInt / 100f)
  }

  override def windowResized(w: Int, h: Int): Unit = {
    super.windowResized(w, h)
    alignTexts(w.toFloat / h)
  }

  private def alignTexts(aspectRatio: Float): Unit = {
    texts.foreach(t => t.setPosition(-aspectRatio + 0.01f * 2 * 16 / 9, t.position.y))
  }
}
