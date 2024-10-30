package hexacraft.client

import hexacraft.gui.{Event, LocationInfo, RenderContext}
import hexacraft.gui.comp.Component
import hexacraft.infra.window.{KeyAction, KeyboardKey, MouseAction}
import hexacraft.util.Channel
import hexacraft.world.Inventory
import hexacraft.world.block.Block

import org.joml.{Matrix4f, Vector4f}

object InventoryBox {
  enum Event {
    case BoxClosed
    case InventoryUpdated(inventory: Inventory)
  }

  def apply(
      currentInventory: Inventory,
      blockTextureIndices: Map[String, IndexedSeq[Int]],
      eventHandler: Channel.Sender[Event]
  ): InventoryBox = {
    val gridRenderer = new GuiBlockRenderer(9, 4)(blockTextureIndices)
    gridRenderer.setViewMatrix(makeTiltedBlockViewMatrix)

    val floatingBlockRenderer = new GuiBlockRenderer(1, 1)(blockTextureIndices)
    floatingBlockRenderer.setViewMatrix(makeTiltedBlockViewMatrix)

    val box = new InventoryBox(currentInventory, gridRenderer, floatingBlockRenderer, eventHandler)
    box.updateRendererContent()
    box
  }

  private def makeTiltedBlockViewMatrix = {
    new Matrix4f()
      .translate(0, 0, -14f)
      .rotateX(3.1415f / 6)
      .rotateY(3.1415f / 24)
      .translate(0, -0.25f, 0)
  }
}

class InventoryBox private (
    private var inventory: Inventory,
    gridRenderer: GuiBlockRenderer,
    floatingBlockRenderer: GuiBlockRenderer,
    eventHandler: Channel.Sender[InventoryBox.Event]
) extends Component {

  private val bounds: LocationInfo = LocationInfo(-4.5f * 0.2f, -2.5f * 0.2f, 9 * 0.2f, 4 * 0.2f)
  private val backgroundColor = new Vector4f(0.4f, 0.4f, 0.4f, 0.75f)
  private val selectedColor = new Vector4f(0.2f, 0.2f, 0.2f, 0.25f)
  private val selectedBox = LocationInfo(bounds.x + 0.01f, bounds.y + 0.01f, 0.18f, 0.18f)

  /** The index of the slot under the cursor */
  private var hoverIndex: Option[Int] = None

  /** The block currently being moved */
  private var floatingBlock: Option[Block] = None

  private def updateRendererContent(): Unit = {
    gridRenderer.updateContent(-4 * 0.2f, -2 * 0.2f, (0 until 9 * 4).map(i => inventory(i)))
    if floatingBlock.isEmpty then {
      floatingBlockRenderer.updateContent(0, 0, Seq(Block.Air))
    }
  }

  private def calculateHoverIndex(mx: Float, my: Float) = {
    if bounds.containsPoint(mx, my) then {
      val xi = ((mx - bounds.x) / 0.2f).toInt
      val yi = ((my - bounds.y) / 0.2f).toInt
      Some(xi + yi * 9)
    } else {
      None
    }
  }

  override def handleEvent(event: Event): Boolean = {
    import Event.*
    event match {
      case KeyEvent(key, _, KeyAction.Press, _) =>
        key match {
          case KeyboardKey.Escape | KeyboardKey.Letter('E') =>
            releaseFloatingBlock()
            eventHandler.send(InventoryBox.Event.BoxClosed)
          case _ =>
        }
      case MouseClickEvent(_, MouseAction.Release, _, mousePos) if bounds.containsPoint(mousePos) =>
        hoverIndex match {
          case Some(hover) =>
            swapFloatingBlock(hover)
          case None =>
            releaseFloatingBlock()
        }
      case _ =>
    }
    true
  }

  private def swapFloatingBlock(index: Int): Unit = {
    val newFloatingBlock = Some(inventory(index)).filter(_ != Block.Air)
    inventory = inventory.updated(index, floatingBlock.getOrElse(Block.Air))
    eventHandler.send(InventoryBox.Event.InventoryUpdated(inventory))
    updateRendererContent()
    floatingBlock = newFloatingBlock
  }

  private def releaseFloatingBlock(): Unit = {
    floatingBlock match {
      case Some(block) =>
        inventory.firstEmptySlot match {
          case Some(slot) =>
            inventory = inventory.updated(slot, block)
            eventHandler.send(InventoryBox.Event.InventoryUpdated(inventory))
            updateRendererContent()
          case None => // TODO: drop the block because the inventory is full
        }
      case None =>
    }
  }

  override def render(context: RenderContext): Unit = {
    gridRenderer.setWindowAspectRatio(context.windowAspectRatio)
    floatingBlockRenderer.setWindowAspectRatio(context.windowAspectRatio)

    val mousePos = context.heightNormalizedMousePos
    hoverIndex = calculateHoverIndex(mousePos.x, mousePos.y)

    if floatingBlock.isDefined then {
      floatingBlockRenderer.updateContent(mousePos.x, mousePos.y, Seq(floatingBlock.get))
    }

    Component.drawRect(bounds, context.offset.x, context.offset.y, backgroundColor, context.windowAspectRatio)

    if hoverIndex.isDefined then {
      val xOffset = context.offset.x + (hoverIndex.get % 9) * 0.2f
      val yOffset = context.offset.y + (hoverIndex.get / 9) * 0.2f
      Component.drawRect(selectedBox, xOffset, yOffset, selectedColor, context.windowAspectRatio)
    }

    gridRenderer.render()

    if floatingBlock.isDefined then {
      floatingBlockRenderer.render()
    }
  }

  override def unload(): Unit = {
    gridRenderer.unload()
    floatingBlockRenderer.unload()
    super.unload()
  }
}
