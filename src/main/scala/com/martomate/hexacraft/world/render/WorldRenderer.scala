package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.renderer.*
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.chunk.ChunkAddedOrRemovedListener
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.coord.integer.ChunkRelWorld

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import org.joml.Vector2ic
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class WorldRenderer(world: BlocksInWorld, initialFramebufferSize: Vector2ic)(using CylinderSize)
    extends ChunkAddedOrRemovedListener:

  Shaders.WorldCombiner.setUniform1i("worldColorTexture", 0)
  Shaders.WorldCombiner.setUniform1i("worldDepthTexture", 1)

  private val chunkHandler: ChunkRenderHandler = new ChunkRenderHandler

  private val skyVAO: VAO = Helpers.makeSkyVAO
  private val skyRenderer = new Renderer(skyVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest

  private val worldCombinerVAO: VAO = Helpers.makeSkyVAO
  private val worldCombinerRenderer = new Renderer(worldCombinerVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest

  private val selectedBlockVAO: VAO = Helpers.makeSelectedBlockVAO
  private val selectedBlockRenderer = new InstancedRenderer(selectedBlockVAO, GL11.GL_LINE_STRIP)

  private var mainFrameBuffer = MainFrameBuffer.fromSize(initialFramebufferSize.x, initialFramebufferSize.y)

  private var currentlySelectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = None

  private val chunksToRender: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val entityRenderers: BlockRendererCollection = new BlockRendererCollection(s => EntityPartRenderer.forSide(s))

  private val chunkRenderUpdater: ChunkRenderUpdater = new ChunkRenderUpdater(updateChunkIfPresent)

  private def updateChunkIfPresent(coords: ChunkRelWorld) =
    world.getChunk(coords) match
      case Some(ch) =>
        val chunkBlocks = ch.blocks.allBlocks
        if !ch.lighting.initialized then ch.lighting.init(chunkBlocks, ch)

        val renderData =
          if chunkBlocks.isEmpty
          then ChunkRenderData.empty
          else ChunkRenderer.getChunkRenderData(ch.coords, chunkBlocks, world)

        chunkHandler.updateHandlers(coords, Some(renderData))
        true
      case None =>
        false

  def tick(camera: Camera, renderDistance: Double): Unit =
    chunkRenderUpdater.update(camera, renderDistance)

  def render(selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])]): Unit =
    // Update the 'selectedBlockVAO' if needed
    if currentlySelectedBlockAndSide != selectedBlockAndSide
    then
      currentlySelectedBlockAndSide = selectedBlockAndSide
      selectedBlockAndSide match
        case Some((coords, Some(_))) =>
          updateSelectedBlockVAO(coords)
        case _ =>

    // Step 1: Render everything to a FrameBuffer
    mainFrameBuffer.bind()

    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)

    Shaders.Sky.enable()
    skyRenderer.render()

    // World content
    chunkHandler.render()
    renderEntities()

    if selectedBlockAndSide.flatMap(_._2).isDefined then
      Shaders.SelectedBlock.enable()
      selectedBlockRenderer.render()

    mainFrameBuffer.unbind()
    GL11.glViewport(0, 0, mainFrameBuffer.frameBuffer.width, mainFrameBuffer.frameBuffer.height)

    // Step 2: Render the FrameBuffer to the screen (one could add post processing here in the future)
    GL13.glActiveTexture(GL13.GL_TEXTURE0)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainFrameBuffer.colorTexture)
    GL13.glActiveTexture(GL13.GL_TEXTURE1)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainFrameBuffer.depthTexture)

    Shaders.WorldCombiner.enable()
    worldCombinerRenderer.render()

    GL13.glActiveTexture(GL13.GL_TEXTURE1)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    GL13.glActiveTexture(GL13.GL_TEXTURE0)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)

  override def onChunkAdded(chunk: Chunk): Unit =
    chunksToRender.add(chunk.coords)
    chunk.addEventListener(chunkRenderUpdater)

  override def onChunkRemoved(chunk: Chunk): Unit =
    chunksToRender.remove(chunk.coords)
    chunkHandler.updateHandlers(chunk.coords, None)
    chunk.removeEventListener(chunkRenderUpdater)

  def framebufferResized(width: Int, height: Int): Unit =
    val newFrameBuffer = MainFrameBuffer.fromSize(width, height)
    mainFrameBuffer.unload()
    mainFrameBuffer = newFrameBuffer

  def unload(): Unit =
    skyVAO.free()
    selectedBlockVAO.free()
    worldCombinerVAO.free()

    entityRenderers.unload()
    chunkHandler.unload()
    mainFrameBuffer.unload()

  private def renderEntities(): Unit =
    for side <- 0 until 8 do
      val sh = if side < 2 then Shaders.Entity else Shaders.EntitySide
      sh.enable()
      sh.setUniform1i("side", side)

      val entityDataList: mutable.ArrayBuffer[EntityDataForShader] = ArrayBuffer.empty
      for
        c <- chunksToRender
        ch <- world.getChunk(c)
        if ch.entities.count > 0
      do entityDataList ++= ChunkRenderer.getEntityRenderData(ch.entities, side, world)

      for (model, partLists) <- entityDataList.groupBy(_.model) do
        val data = partLists.flatMap(_.parts)

        entityRenderers.updateContent(side, data.size) { buf =>
          data.foreach(_.fill(buf))
        }
        model.texture.bind()
        sh.setUniform1i("texSize", model.texSize)
        entityRenderers.renderBlockSide(side)

  private def updateSelectedBlockVAO(coords: BlockRelWorld) =
    val blockState = world.getBlock(coords)

    val buf = BufferUtils
      .createByteBuffer(7 * 4)
      .putInt(coords.x)
      .putInt(coords.y)
      .putInt(coords.z)
      .putFloat(0)
      .putFloat(0)
      .putFloat(0)
      .putFloat(blockState.blockType.blockHeight(blockState.metadata))

    buf.flip()
    selectedBlockVAO.vbos(1).fill(0, buf)

  private object Helpers:
    def makeSelectedBlockVAO: VAO =
      def expandFn(v: CylCoords.Offset): Seq[Float] =
        Seq(v.x * 1.0025, (v.y - 0.25) * 1.0025 + 0.25, v.z * 1.0025).map(_.toFloat)

      def fn(s: Int): Seq[Float] = BlockState.getVertices(s + 2).flatMap(expandFn)

      val vertexData = Seq(0, 2, 4).flatMap(fn) ++ expandFn(BlockState.vertices.head) ++ Seq(1, 3, 5).flatMap(fn)

      new VAOBuilder(25)
        .addVBO(
          VBOBuilder(25)
            .floats(0, 3)
            .create()
            .fillFloats(0, vertexData)
        )
        .addVBO(
          VBOBuilder(1, GL15.GL_DYNAMIC_DRAW, 1)
            .ints(1, 3)
            .floats(2, 3)
            .floats(3, 1)
            .create()
        )
        .create()

    def makeSkyVAO: VAO =
      new VAOBuilder(4)
        .addVBO(
          VBOBuilder(4)
            .floats(0, 2)
            .create()
            .fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
        )
        .create()
