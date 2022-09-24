package com.martomate.hexacraft.gui.inventory

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{LocationInfo, LocationInfoIdentity}
import com.martomate.hexacraft.gui.comp._
import com.martomate.hexacraft.world.player.Inventory
import org.joml.{Matrix4f, Vector4f}

class Toolbar(location: LocationInfo, inventory: Inventory)(implicit window: GameWindow)
    extends Component
    with SubComponents {
  private val backgroundColor = new Vector4f(0.4f, 0.4f, 0.4f, 0.75f)
  private val selectedColor = new Vector4f(0.2f, 0.2f, 0.2f, 0.25f)
  private val selectedBox = LocationInfoIdentity(
    location.x + location.h * 0.05f,
    location.y + location.h * 0.05f,
    location.w / 9 - location.h * 0.1f,
    location.h - location.h * 0.1f
  )
  private var selectedIndex = 0
  def setSelectedIndex(idx: Int): Unit = selectedIndex = idx

  private val guiBlockRenderer =
    new GUIBlocksRenderer(9, 1, 0.2f)(x => inventory(x), () => (-4 * 0.2f, -0.83f))
  guiBlockRenderer.setViewMatrix(
    new Matrix4f()
      .translate(0, 0, -14f)
      .rotateX(3.1415f / 6)
      .rotateY(3.1415f / 24)
      .translate(0, -0.25f, 0)
  )

  inventory.addChangeListener(() => guiBlockRenderer.updateContent())

  override def render(transformation: GUITransformation): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, backgroundColor)
    Component.drawRect(
      selectedBox,
      transformation.x + selectedIndex * location.w / 9,
      transformation.y,
      selectedColor
    )

    guiBlockRenderer.render(transformation)
  }

  override def unload(): Unit = {
    guiBlockRenderer.unload()
    super.unload()
  }
}
