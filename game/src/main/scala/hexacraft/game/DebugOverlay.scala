package hexacraft.game

import hexacraft.gui.{LocationInfo, RenderContext}
import hexacraft.gui.comp.{Component, GUITransformation}
import hexacraft.text.{Text, TextMaster}
import hexacraft.world.{Camera, CylinderSize}
import hexacraft.world.coord.{ChunkRelWorld, CylCoords}

import org.joml.Vector3f

import scala.collection.mutable

object DebugOverlay {
  case class Content(
      cameraPosition: CylCoords,
      cameraChunkCoords: ChunkRelWorld,
      cameraRotation: Vector3f,
      viewDistance: Double,
      regularChunkBufferFragmentation: IndexedSeq[Float],
      transmissiveChunkBufferFragmentation: IndexedSeq[Float]
  )

  object Content {
    def fromCamera(
        camera: Camera,
        viewDistance: Double,
        regularChunkBufferFragmentation: IndexedSeq[Float],
        transmissiveChunkBufferFragmentation: IndexedSeq[Float]
    )(using CylinderSize): Content = {
      Content(
        CylCoords(camera.position),
        camera.blockCoords.getChunkRelWorld,
        camera.rotation,
        viewDistance,
        regularChunkBufferFragmentation,
        transmissiveChunkBufferFragmentation
      )
    }
  }
}

class DebugOverlay {
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
  addDebugText("fragmentation.opaque", "fragmentation (opaque)")
  addDebugText("fragmentation.transmissive", "fragmentation (transmissive)")

  private def addLabel(text: String): Unit = {
    yOff += 0.02f

    val location = LocationInfo.from16x9(0.01f, 0.95f - yOff, 0.2f, 0.05f)
    val guiText = Component.makeText(text, location, 2, centered = false, shadow = true)
    textMaster.loadText(guiText)
    texts += guiText

    yOff += 0.03f
  }

  private def addDebugText(id: String, display: String): Unit = {
    val location = LocationInfo.from16x9(0.01f, 0.95f - yOff, 0.5f, 0.05f)
    val guiText = Component.makeText("", location, 2, centered = false, shadow = true)
    textMaster.loadText(guiText)
    texts += guiText

    textDisplayMap += id -> display
    textValueMap += id -> guiText

    yOff += 0.03f
  }

  private def setValue(name: String, value: Any): Unit = {
    textValueMap(name).setText(textDisplayMap(name) + ": " + value)
  }

  def updateContent(info: DebugOverlay.Content): Unit = {
    setValue("p.x", info.cameraPosition.x.toFloat)
    setValue("p.y", info.cameraPosition.y.toFloat)
    setValue("p.z", info.cameraPosition.z.toFloat)

    setValue("c.x", info.cameraChunkCoords.X.toInt)
    setValue("c.y", info.cameraChunkCoords.Y.toInt)
    setValue("c.z", info.cameraChunkCoords.Z.toInt)

    setValue("r.x", info.cameraRotation.x)
    setValue("r.y", info.cameraRotation.y)
    setValue("r.z", info.cameraRotation.z)

    setValue("viewDist", f"${info.viewDistance}%.2f")
    setValue(
      "fragmentation.opaque",
      info.regularChunkBufferFragmentation.sortBy(-_).map(v => f"$v%.2f").mkString(" ")
    )
    setValue(
      "fragmentation.transmissive",
      info.transmissiveChunkBufferFragmentation.sortBy(-_).map(v => f"$v%.2f").mkString(" ")
    )
  }

  def render(transformation: GUITransformation)(using context: RenderContext): Unit = {
    texts.foreach(t => t.setPosition(-context.windowAspectRatio + 0.01f * 2 * 16 / 9, t.position.y))
    textMaster.setWindowAspectRatio(context.windowAspectRatio)
    textMaster.render(transformation.x, transformation.y)
  }

  def unload(): Unit = {
    textMaster.unload()
  }
}
