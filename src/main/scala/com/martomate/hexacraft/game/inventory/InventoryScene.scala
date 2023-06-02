package com.martomate.hexacraft.game.inventory

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.game.inventory.GUIBlocksRenderer
import com.martomate.hexacraft.gui.{Event, LocationInfo, Scene}
import com.martomate.hexacraft.gui.comp.{Component, GUITransformation}
import com.martomate.hexacraft.world.block.{Block, Blocks}
import com.martomate.hexacraft.world.player.Inventory

import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.glfw.GLFW

class InventoryScene(inventory: Inventory, closeScene: () => Unit)(using window: GameWindow, Blocks: Blocks)
    extends Scene {
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
    val mousePos = window.normalizedMousePos
    val (mx, my) = (mousePos.x * window.aspectRatio, mousePos.y)
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
    guiBlockRenderer.onWindowAspectRatioChanged(w.toFloat / h)

  override def onMouseClickEvent(event: Event.MouseClickEvent): Boolean = {
    if event.action == Event.MouseAction.Release && location.containsPoint(event.mousePos)
    then
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
    true
  }

  override def onKeyEvent(event: Event.KeyEvent): Boolean = {
    if event.action == Event.KeyAction.Press
    then
      event.key match
        case GLFW.GLFW_KEY_ESCAPE | GLFW.GLFW_KEY_E =>
          handleFloatingBlock()
          closeScene()
        case _ =>
    true
  }

  private def handleFloatingBlock(): Unit = {
    floatingBlock match
      case Some(block) =>
        firstEmptySlot match
          case Some(slot) => inventory(slot) = block
          case None       => // TODO: drop the block because the inventory is full
      case None =>
  }

  private def firstEmptySlot = (0 until 4 * 9).find(i => inventory(i) == Blocks.Air)

  override def render(transformation: GUITransformation): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, backgroundColor)

    if hoverIndex.isDefined
    then
      val xOffset = transformation.x + (hoverIndex.get % 9) * 0.2f
      val yOffset = transformation.y + (hoverIndex.get / 9) * 0.2f
      Component.drawRect(selectedBox, xOffset, yOffset, selectedColor)

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
    renderer.onWindowAspectRatioChanged(window.aspectRatio)
    renderer
  }

  private def makeFloatingBlockRenderer() = {
    val blockProvider = () => floatingBlock.getOrElse(Blocks.Air)
    val rendererLocation = () =>
      val mousePos = window.normalizedMousePos
      (mousePos.x * window.aspectRatio, mousePos.y)

    val individualBlockViewMatrix = makeTiltedBlockViewMatrix

    val renderer = GUIBlocksRenderer.withSingleSlot(blockProvider, rendererLocation)
    renderer.setViewMatrix(individualBlockViewMatrix)
    renderer.onWindowAspectRatioChanged(window.aspectRatio)
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
