package hexacraft.game.inventory

import hexacraft.gui.{LocationInfo, RenderContext}
import hexacraft.gui.comp.{Component, GUITransformation, SubComponents}
import hexacraft.world.block.BlockSpecRegistry
import hexacraft.world.player.Inventory

import org.joml.{Matrix4f, Vector4f}

class Toolbar(location: LocationInfo, private var inventory: Inventory)(specs: BlockSpecRegistry)
    extends Component
    with SubComponents {
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
    val renderer = new GuiBlockRenderer(9, 1)(specs)
    renderer.setViewMatrix(
      new Matrix4f()
        .translate(0, 0, -14f)
        .rotateX(3.1415f / 6)
        .rotateY(3.1415f / 24)
        .translate(0, -0.25f, 0)
    )
    renderer

  updateRendererContent()

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    guiBlockRenderer.setWindowAspectRatio(aspectRatio)

  def onInventoryUpdated(inventory: Inventory): Unit =
    this.inventory = inventory
    updateRendererContent()

  private def updateRendererContent(): Unit =
    guiBlockRenderer.updateContent(-4 * 0.2f, -0.83f, (0 until 9).map(i => inventory(i)))

  override def render(transformation: GUITransformation)(using context: RenderContext): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, backgroundColor, context.windowAspectRatio)
    Component.drawRect(
      selectedBox,
      transformation.x + selectedIndex * location.w / 9,
      transformation.y,
      selectedColor,
      context.windowAspectRatio
    )

    guiBlockRenderer.render(transformation)
  }

  override def unload(): Unit = {
    guiBlockRenderer.unload()
    super.unload()
  }
}
