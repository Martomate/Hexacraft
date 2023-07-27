package com.martomate.hexacraft.game.inventory

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.LocationInfo
import com.martomate.hexacraft.gui.comp.*
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.player.Inventory
import org.joml.{Matrix4f, Vector4f}

class Toolbar(location: LocationInfo, inventory: Inventory)(using Blocks: Blocks) extends Component with SubComponents {
  private val backgroundColor = new Vector4f(0.4f, 0.4f, 0.4f, 0.75f)
  private val selectedColor = new Vector4f(0.2f, 0.2f, 0.2f, 0.25f)
  private val selectedBox = LocationInfo(
    location.x + location.h * 0.05f,
    location.y + location.h * 0.05f,
    location.w / 9 - location.h * 0.1f,
    location.h - location.h * 0.1f
  )
  private var selectedIndex = 0
  def setSelectedIndex(idx: Int): Unit = selectedIndex = idx

  private val guiBlockRenderer =
    val renderer = new GUIBlocksRenderer(9, 1, 0.2f)(x => inventory(x), () => (-4 * 0.2f, -0.83f))
    renderer.setViewMatrix(
      new Matrix4f()
        .translate(0, 0, -14f)
        .rotateX(3.1415f / 6)
        .rotateY(3.1415f / 24)
        .translate(0, -0.25f, 0)
    )
    renderer

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    guiBlockRenderer.setWindowAspectRatio(aspectRatio)

  private val revokeInventoryTracker = inventory.trackChanges(_ => guiBlockRenderer.updateContent())

  override def render(transformation: GUITransformation)(using window: GameWindow): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, backgroundColor, window.aspectRatio)
    Component.drawRect(
      selectedBox,
      transformation.x + selectedIndex * location.w / 9,
      transformation.y,
      selectedColor,
      window.aspectRatio
    )

    guiBlockRenderer.render(transformation)
  }

  override def unload(): Unit = {
    revokeInventoryTracker()
    guiBlockRenderer.unload()
    super.unload()
  }
}
