package hexacraft.game.inventory

import hexacraft.gui.comp.GUITransformation
import hexacraft.renderer.{Shader, TextureArray}
import hexacraft.world.block.{Block, Blocks}
import hexacraft.world.camera.CameraProjection
import hexacraft.world.render.{BlockRendererCollection, FlatBlockRenderer}

import org.joml.{Matrix4f, Vector3f}

object GUIBlocksRenderer:
  def withSingleSlot(
      blockProvider: () => Block,
      rendererLocation: () => (Float, Float) = () => (0, 0),
      brightnessFunc: () => Float = () => 1.0f
  )(using Blocks: Blocks): GUIBlocksRenderer =
    new GUIBlocksRenderer(1, 1)(_ => blockProvider(), rendererLocation, (_, _) => brightnessFunc())

  private val guiBlockShader = new GuiBlockShader(isSide = false)
  private val guiBlockSideShader = new GuiBlockShader(isSide = true)

class GUIBlocksRenderer(w: Int, h: Int = 1, separation: Float = 0.2f)(
    blockProvider: Int => Block,
    rendererLocation: () => (Float, Float) = () => (0, 0),
    brightnessFunc: (Int, Int) => Float = (_, _) => 1.0f
)(using Blocks: Blocks):
  private val guiBlockRenderer = new BlockRendererCollection(s => FlatBlockRenderer.forSide(s))
  private val guiBlockShader = GUIBlocksRenderer.guiBlockShader
  private val guiBlockSideShader = GUIBlocksRenderer.guiBlockSideShader
  private val blockTexture: TextureArray = TextureArray.getTextureArray("blocks")

  private var viewMatrix = new Matrix4f
  def setViewMatrix(matrix: Matrix4f): Unit = viewMatrix = matrix

  private val cameraProjection = new CameraProjection(70f, 16f / 9f, 0.02f, 1000)

  def setWindowAspectRatio(aspectRatio: Float): Unit =
    cameraProjection.aspect = aspectRatio
    cameraProjection.updateProjMatrix()
    guiBlockShader.setWindowAspectRatio(aspectRatio)
    guiBlockSideShader.setWindowAspectRatio(aspectRatio)

  updateContent()

  def render(transformation: GUITransformation): Unit =
    guiBlockShader.setViewMatrix(viewMatrix)
    guiBlockSideShader.setViewMatrix(viewMatrix)

    guiBlockShader.setProjectionMatrix(cameraProjection.matrix)
    guiBlockSideShader.setProjectionMatrix(cameraProjection.matrix)

    blockTexture.bind()

    for (side <- 0 until 8)
      val sh = if side < 2 then guiBlockShader else guiBlockSideShader
      sh.enable()
      sh.setSide(side)
      guiBlockRenderer.renderBlockSide(side)

  def updateContent(): Unit =
    val (xOff, yOff) = rendererLocation()

    for (side <- 0 until 8)
      guiBlockRenderer.updateContent(side, 9 * 9) { buf =>
        for (y <- 0 until h)
          for (x <- 0 until w)
            val blockToDraw = blockProvider(x + y * w)
            if blockToDraw.canBeRendered
            then
              buf.putFloat(x * separation + xOff)
              buf.putFloat(y * separation + yOff)
              buf.putInt(Blocks.textures(blockToDraw.name)(side))
              buf.putFloat(
                1.0f
              ) // blockInHand.blockHeight(new BlockState(BlockRelWorld(0, 0, 0, world), blockInHand)))
              buf.putFloat(1) // brightnessFunc(x, y))
      }

  def unload(): Unit =
    guiBlockRenderer.unload()
