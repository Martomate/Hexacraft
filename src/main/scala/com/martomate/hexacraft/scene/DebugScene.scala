package com.martomate.hexacraft.scene

import com.martomate.hexacraft.Main
import com.martomate.hexacraft.gui.comp.{Component, LocationInfo}
import fontMeshCreator.GUIText

import scala.collection.mutable

class DebugScene(gameScene: GameScene) extends Scene {
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

  alignTexts(Main.aspectRatio)

  override def isOpaque: Boolean = false

  private def addLabel(text: String): Unit = {
    yOff += 0.02f
    val guiText = Component.makeText(text, LocationInfo(0.01f, 0.95f - yOff, 0.2f, 0.05f), 2, centered = false)
    addText(guiText)
    yOff += 0.03f
    texts += guiText
  }

  private def addDebugText(id: String, display: String, defaultValue: String = ""): Unit = {
    val text = Component.makeText(defaultValue, LocationInfo(0.01f, 0.95f - yOff, 0.2f, 0.05f), 2, centered = false)
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
    setValue("p.x", gameScene.camera.position.x.toFloat)
    setValue("p.y", gameScene.camera.position.y.toFloat)
    setValue("p.z", gameScene.camera.position.z.toFloat)

    setValue("c.x", gameScene.camera.blockCoords.x >> 4)
    setValue("c.y", gameScene.camera.blockCoords.y >> 4)
    setValue("c.z", gameScene.camera.blockCoords.z >> 4)

    setValue("r.x", gameScene.camera.rotation.x)
    setValue("r.y", gameScene.camera.rotation.y)
    setValue("r.z", gameScene.camera.rotation.z)

    setValue("viewDist", (100 * gameScene.world.renderDistance).toInt / 100f)
  }

  override def windowResized(w: Int, h: Int): Unit = {
    super.windowResized(w, h)
    alignTexts(w.toFloat / h)
  }

  private def alignTexts(aspectRatio: Float): Unit = {
    texts.foreach(t => t.setPosition(-aspectRatio + 0.01f * 2 * 16 / 9, t.getPosition.y))
  }
}
