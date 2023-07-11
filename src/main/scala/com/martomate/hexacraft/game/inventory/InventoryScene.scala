package com.martomate.hexacraft.game.inventory

import com.martomate.hexacraft.{GameMouse, GameWindow}
import com.martomate.hexacraft.game.inventory.GUIBlocksRenderer
import com.martomate.hexacraft.gui.{Event, LocationInfo, Scene}
import com.martomate.hexacraft.gui.comp.{Component, GUITransformation}
import com.martomate.hexacraft.infra.window.{KeyAction, KeyboardKey, MouseAction}
import com.martomate.hexacraft.world.block.{Block, Blocks}
import com.martomate.hexacraft.world.player.Inventory

import org.joml.{Matrix4f, Vector4f}

class InventoryScene(inventory: Inventory, closeScene: () => Unit)(using
    mouse: GameMouse,
    window: GameWindow,
    Blocks: Blocks
) extends Scene {
  private val location: LocationInfo = LocationInfo(-4.5f * 0.2f, -2.5f * 0.2f, 9 * 0.2f, 4 * 0.2f)
  private val backgroundColor = new Vector4f(0.4f, 0.4f, 0.4f, 0.75f)
  private val selectedColor = new Vector4f(0.2f, 0.2f, 0.2f, 0.25f)
  private val selectedBox = LocationInfo(location.x + 0.01f, location.y + 0.01f, 0.18f, 0.18f)

  /** The index of the slot under the cursor */
  private var hoverIndex: Option[Int] = None

  /** The block currently being moved */
  private var floatingBlock: Option[Block] = None

  private val guiBlockRenderer = makeGuiBlockRenderer()
  private val floatingBlockRenderer = makeFloatingBlockRenderer()

  private val revokeInventoryTracker = inventory.trackChanges(_ => {
    guiBlockRenderer.updateContent()
    floatingBlockRenderer.updateContent()
  })

  override def isOpaque: Boolean = false

  override def tick(): Unit = {
    val mousePos = mouse.heightNormalizedPos(window.windowSize)
    val (mx, my) = (mousePos.x, mousePos.y)
    if location.containsPoint(mx, my)
    then
      val xi = ((mx - location.x) / 0.2f).toInt
      val yi = ((my - location.y) / 0.2f).toInt
      hoverIndex = Some(xi + yi * 9)
    else hoverIndex = None

    if floatingBlock.isDefined
    then floatingBlockRenderer.updateContent()
  }

  override def windowResized(w: Int, h: Int): Unit =
    guiBlockRenderer.setWindowAspectRatio(w.toFloat / h)

  override def handleEvent(event: Event): Boolean =
    import Event.*
    event match
      case KeyEvent(key, _, KeyAction.Press, _) =>
        key match
          case KeyboardKey.Escape | KeyboardKey.Letter('E') =>
            handleFloatingBlock()
            closeScene()
          case _ =>
      case MouseClickEvent(_, MouseAction.Release, _, mousePos) if location.containsPoint(mousePos) =>
        hoverIndex match
          case Some(hover) =>
            val newFloatingBlock = Some(inventory(hover)).filter(_ != Blocks.Air)
            floatingBlock match
              case Some(block) =>
                floatingBlock = newFloatingBlock
                inventory(hover) = block
              case None =>
                floatingBlock = newFloatingBlock
                inventory(hover) = Blocks.Air
          case None =>
            handleFloatingBlock()
      case _ =>
    true

  private def handleFloatingBlock(): Unit =
    floatingBlock match
      case Some(block) =>
        firstEmptySlot match
          case Some(slot) => inventory(slot) = block
          case None       => // TODO: drop the block because the inventory is full
      case None =>

  private def firstEmptySlot = (0 until 4 * 9).find(i => inventory(i) == Blocks.Air)

  override def render(transformation: GUITransformation)(using window: GameWindow): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, backgroundColor, window.aspectRatio)

    if hoverIndex.isDefined
    then
      val xOffset = transformation.x + (hoverIndex.get % 9) * 0.2f
      val yOffset = transformation.y + (hoverIndex.get / 9) * 0.2f
      Component.drawRect(selectedBox, xOffset, yOffset, selectedColor, window.aspectRatio)

    guiBlockRenderer.render(transformation)

    if floatingBlock.isDefined
    then floatingBlockRenderer.render(transformation)
  }

  override def unload(): Unit = {
    revokeInventoryTracker()
    guiBlockRenderer.unload()
    floatingBlockRenderer.unload()
    super.unload()
  }

  private def makeGuiBlockRenderer() = {
    val blockProvider = (i: Int) => inventory(i)
    val rendererLocation = () => (-4 * 0.2f, -2 * 0.2f)
    val individualBlockViewMatrix = makeTiltedBlockViewMatrix

    val renderer = new GUIBlocksRenderer(9, 4, 0.2f)(blockProvider, rendererLocation)
    renderer.setViewMatrix(individualBlockViewMatrix)
    renderer.setWindowAspectRatio(window.aspectRatio)
    renderer
  }

  private def makeFloatingBlockRenderer() = {
    val blockProvider = () => floatingBlock.getOrElse(Blocks.Air)
    val rendererLocation = () =>
      val mousePos = mouse.heightNormalizedPos(window.windowSize)
      (mousePos.x, mousePos.y)

    val individualBlockViewMatrix = makeTiltedBlockViewMatrix

    val renderer = GUIBlocksRenderer.withSingleSlot(blockProvider, rendererLocation)
    renderer.setViewMatrix(individualBlockViewMatrix)
    renderer.setWindowAspectRatio(window.aspectRatio)
    renderer
  }

  private def makeTiltedBlockViewMatrix = {
    new Matrix4f()
      .translate(0, 0, -14f)
      .rotateX(3.1415f / 6)
      .rotateY(3.1415f / 24)
      .translate(0, -0.25f, 0)
  }
}
