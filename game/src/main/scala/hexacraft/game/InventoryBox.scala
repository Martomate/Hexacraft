package hexacraft.game

import hexacraft.gui.{Event, LocationInfo, RenderContext}
import hexacraft.gui.comp.{Component, GUITransformation}
import hexacraft.infra.window.{KeyAction, KeyboardKey, MouseAction}
import hexacraft.util.Tracker
import hexacraft.world.Inventory
import hexacraft.world.block.{Block, BlockSpecRegistry}

import org.joml.{Matrix4f, Vector4f}

object InventoryBox {
  enum Event:
    case BoxClosed
    case InventoryUpdated(inventory: Inventory)

  def apply(currentInventory: Inventory, specs: BlockSpecRegistry)(eventHandler: Tracker[Event]): InventoryBox =
    val gridRenderer = new GuiBlockRenderer(9, 4)(specs)
    gridRenderer.setViewMatrix(makeTiltedBlockViewMatrix)

    val floatingBlockRenderer = new GuiBlockRenderer(1, 1)(specs)
    floatingBlockRenderer.setViewMatrix(makeTiltedBlockViewMatrix)

    val box = new InventoryBox(currentInventory, gridRenderer, floatingBlockRenderer, eventHandler)
    box.updateRendererContent()
    box

  private def makeTiltedBlockViewMatrix =
    new Matrix4f()
      .translate(0, 0, -14f)
      .rotateX(3.1415f / 6)
      .rotateY(3.1415f / 24)
      .translate(0, -0.25f, 0)
}

class InventoryBox private (
    private var inventory: Inventory,
    gridRenderer: GuiBlockRenderer,
    floatingBlockRenderer: GuiBlockRenderer,
    eventHandler: Tracker[InventoryBox.Event]
) extends Component {

  private val location: LocationInfo = LocationInfo(-4.5f * 0.2f, -2.5f * 0.2f, 9 * 0.2f, 4 * 0.2f)
  private val backgroundColor = new Vector4f(0.4f, 0.4f, 0.4f, 0.75f)
  private val selectedColor = new Vector4f(0.2f, 0.2f, 0.2f, 0.25f)
  private val selectedBox = LocationInfo(location.x + 0.01f, location.y + 0.01f, 0.18f, 0.18f)

  /** The index of the slot under the cursor */
  private var hoverIndex: Option[Int] = None

  /** The block currently being moved */
  private var floatingBlock: Option[Block] = None

  private def updateRendererContent(): Unit =
    gridRenderer.updateContent(-4 * 0.2f, -2 * 0.2f, (0 until 9 * 4).map(i => inventory(i)))
    if floatingBlock.isEmpty then floatingBlockRenderer.updateContent(0, 0, Seq(Block.Air))

  private def calculateHoverIndex(mx: Float, my: Float) =
    if location.containsPoint(mx, my)
    then
      val xi = ((mx - location.x) / 0.2f).toInt
      val yi = ((my - location.y) / 0.2f).toInt
      Some(xi + yi * 9)
    else None

  override def handleEvent(event: Event): Boolean =
    import Event.*
    event match
      case KeyEvent(key, _, KeyAction.Press, _) =>
        key match
          case KeyboardKey.Escape | KeyboardKey.Letter('E') =>
            handleFloatingBlock()
            eventHandler.notify(InventoryBox.Event.BoxClosed)
          case _ =>
      case MouseClickEvent(_, MouseAction.Release, _, mousePos) if location.containsPoint(mousePos) =>
        hoverIndex match
          case Some(hover) =>
            val newFloatingBlock = Some(inventory(hover)).filter(_ != Block.Air)
            inventory = inventory.updated(hover, floatingBlock.getOrElse(Block.Air))
            eventHandler.notify(InventoryBox.Event.InventoryUpdated(inventory))
            updateRendererContent()
            floatingBlock = newFloatingBlock
          case None =>
            handleFloatingBlock()
      case _ =>
    true

  private def handleFloatingBlock(): Unit =
    floatingBlock match
      case Some(block) =>
        inventory.firstEmptySlot match
          case Some(slot) =>
            inventory = inventory.updated(slot, block)
            eventHandler.notify(InventoryBox.Event.InventoryUpdated(inventory))
            updateRendererContent()
          case None => // TODO: drop the block because the inventory is full
      case None =>

  override def render(transformation: GUITransformation)(using context: RenderContext): Unit =
    gridRenderer.setWindowAspectRatio(context.windowAspectRatio)
    floatingBlockRenderer.setWindowAspectRatio(context.windowAspectRatio)

    val mousePos = context.heightNormalizedMousePos
    hoverIndex = calculateHoverIndex(mousePos.x, mousePos.y)

    if floatingBlock.isDefined
    then floatingBlockRenderer.updateContent(mousePos.x, mousePos.y, Seq(floatingBlock.get))

    Component.drawRect(location, transformation.x, transformation.y, backgroundColor, context.windowAspectRatio)

    if hoverIndex.isDefined
    then
      val xOffset = transformation.x + (hoverIndex.get % 9) * 0.2f
      val yOffset = transformation.y + (hoverIndex.get / 9) * 0.2f
      Component.drawRect(selectedBox, xOffset, yOffset, selectedColor, context.windowAspectRatio)

    gridRenderer.render(transformation)

    if floatingBlock.isDefined
    then floatingBlockRenderer.render(transformation)

  override def unload(): Unit =
    gridRenderer.unload()
    floatingBlockRenderer.unload()
    super.unload()
}
