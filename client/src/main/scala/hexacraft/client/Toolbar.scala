package hexacraft.client

import hexacraft.gui.{LocationInfo, RenderContext}
import hexacraft.gui.comp.Component
import hexacraft.world.Inventory

import org.joml.{Matrix4f, Vector4f}

class Toolbar(location: LocationInfo, private var inventory: Inventory)(
    blockTextureIndices: Map[String, IndexedSeq[Int]]
) extends Component {
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
    val renderer = new GuiBlockRenderer(9, 1)(blockTextureIndices)
    renderer.setViewMatrix(
      new Matrix4f()
        .translate(0, 0, -14f)
        .rotateX(3.1415f / 6)
        .rotateY(3.1415f / 24)
        .translate(0, -0.25f, 0)
    )
    renderer

  updateRendererContent()

  def setWindowAspectRatio(aspectRatio: Float): Unit = {
    guiBlockRenderer.setWindowAspectRatio(aspectRatio)
  }

  def onInventoryUpdated(inventory: Inventory): Unit = {
    this.inventory = inventory
    updateRendererContent()
  }

  private def updateRendererContent(): Unit = {
    guiBlockRenderer.updateContent(-4 * 0.2f, -0.83f, (0 until 9).map(i => inventory(i)))
  }

  override def render(context: RenderContext): Unit = {
    Component.drawRect(location, context.offset.x, context.offset.y, backgroundColor, context.windowAspectRatio)
    Component.drawRect(
      selectedBox,
      context.offset.x + selectedIndex * location.w / 9,
      context.offset.y,
      selectedColor,
      context.windowAspectRatio
    )

    guiBlockRenderer.render()
  }

  override def unload(): Unit = {
    guiBlockRenderer.unload()
    super.unload()
  }
}
