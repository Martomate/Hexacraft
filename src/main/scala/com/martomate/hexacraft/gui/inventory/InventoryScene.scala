package com.martomate.hexacraft.gui.inventory

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.gui.{CharEvent, KeyEvent, MouseClickEvent}
import com.martomate.hexacraft.gui.comp.*
import com.martomate.hexacraft.gui.location.{LocationInfo, LocationInfoIdentity}
import com.martomate.hexacraft.scene.Scene
import com.martomate.hexacraft.world.block.{Block, Blocks}
import com.martomate.hexacraft.world.player.Inventory
import org.joml.{Matrix4f, Vector4f}
import org.lwjgl.glfw.GLFW

class InventoryScene(inventory: Inventory, closeScene: () => Unit)(implicit
    window: GameWindow
) extends Scene {
  private val location: LocationInfo =
    new LocationInfoIdentity(-4.5f * 0.2f, -2.5f * 0.2f, 9 * 0.2f, 4 * 0.2f)
  private val backgroundColor = new Vector4f(0.4f, 0.4f, 0.4f, 0.75f)
  private val selectedColor = new Vector4f(0.2f, 0.2f, 0.2f, 0.25f)
  private val selectedBox = new LocationInfoIdentity(
    location.x + 0.01f,
    location.y + 0.01f,
    0.18f,
    0.18f
  )

  /** The index of the slot under the cursor */
  private var hoverIndex: Option[Int] = None

  /** The block currently being moved */
  private var floatingBlock: Option[Block] = None

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

  override def onMouseClickEvent(event: MouseClickEvent): Boolean = {
    if event.action == GLFW.GLFW_RELEASE && location.containsPoint(event.mousePos)
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

  override def onKeyEvent(event: KeyEvent): Boolean = {
    if event.action == GLFW.GLFW_PRESS then
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

  private val guiBlockRenderer =
    new GUIBlocksRenderer(
      9,
      4,
      0.2f,
      () => (-4 * 0.2f, -2 * 0.2f)
    )(i => inventory(i))
  guiBlockRenderer.setViewMatrix(
    new Matrix4f()
      .translate(0, 0, -14f)
      .rotateX(3.1415f / 6)
      .rotateY(3.1415f / 24)
      .translate(0, -0.25f, 0)
  )

  private val floatingBlockRenderer =
    new GUIBlocksRenderer(
      1,
      1,
      0,
      () => {
        val mousePos = window.normalizedMousePos
        (mousePos.x * window.aspectRatio, mousePos.y)
      }
    )(_ => floatingBlock.getOrElse(Blocks.Air))
  floatingBlockRenderer.setViewMatrix(
    new Matrix4f()
      .translate(0, 0, -14f)
      .rotateX(3.1415f / 6)
      .rotateY(3.1415f / 24)
      .translate(0, -0.25f, 0)
  )

  inventory.addChangeListener(() => guiBlockRenderer.updateContent())
  inventory.addChangeListener(() => floatingBlockRenderer.updateContent())

  override def render(transformation: GUITransformation): Unit = {
    Component.drawRect(location, transformation.x, transformation.y, backgroundColor)

    if (hoverIndex.isDefined)
      Component.drawRect(
        selectedBox,
        transformation.x + (hoverIndex.get % 9) * 0.2f,
        transformation.y + (hoverIndex.get / 9) * 0.2f,
        selectedColor
      )

    guiBlockRenderer.render(transformation)

    if floatingBlock.isDefined
    then floatingBlockRenderer.render(transformation)
  }

  override def unload(): Unit = {
    guiBlockRenderer.unload()
    super.unload()
  }
}
