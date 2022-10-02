package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.GameWindow
import com.martomate.hexacraft.renderer._
import com.martomate.hexacraft.resource.{Shaders, TextureSingle}
import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.BlockState
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.{ChunkAddedOrRemovedListener, Chunk}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl._

import java.nio.{ByteBuffer, FloatBuffer}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class WorldRenderer(world: BlocksInWorld, renderDistance: => Double)(implicit
    window: GameWindow,
    cylSize: CylinderSize
) extends ChunkAddedOrRemovedListener {
  private val entityShader = Shaders.Entity
  private val entitySideShader = Shaders.EntitySide
  private val skyShader = Shaders.Sky
  private val selectedBlockShader = Shaders.SelectedBlock
  private val worldCombinerShader = Shaders.WorldCombiner

  worldCombinerShader.setUniform1i("worldColorTexture", 0)
  worldCombinerShader.setUniform1i("worldDepthTexture", 1)

  private val chunkHandler: ChunkRenderHandler = new ChunkRenderHandler

  private val skyVAO: VAO = makeSkyVAO
  private val skyRenderer = new Renderer(skyVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest

  private val worldCombinerVAO: VAO = makeSkyVAO
  private val worldCombinerRenderer = new Renderer(worldCombinerVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest

  private val selectedBlockVAO: VAO = makeSelectedBlockVAO
  private val selectedBlockRenderer = new InstancedRenderer(selectedBlockVAO, GL11.GL_LINE_STRIP)

  private var mainColorTexture =
    makeMainColorTexture(window.framebufferSize.x, window.framebufferSize.y)
  private var mainDepthTexture =
    makeMainDepthTexture(window.framebufferSize.x, window.framebufferSize.y)
  private var mainFrameBuffer = makeMainFrameBuffer(
    mainColorTexture,
    mainDepthTexture,
    window.framebufferSize.x,
    window.framebufferSize.y
  )

  private var _selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = None
  def selectedSide: Option[Int] = _selectedBlockAndSide.flatMap(_._2)
  def selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = _selectedBlockAndSide

  def selectedBlockAndSide_=(blockAndSide: Option[(BlockRelWorld, Option[Int])]): Unit = {
    if (_selectedBlockAndSide != blockAndSide) {
      _selectedBlockAndSide = blockAndSide

      blockAndSide match {
        case Some((coords, Some(_))) =>
          updateSelectedBlockVAO(coords)
        case _ =>
      }
    }
  }

  private val chunkRenderers: mutable.Map[ChunkRelWorld, ChunkRenderer] = mutable.HashMap.empty
  private val entityRenderers: BlockRendererCollection[EntityPartRenderer] =
    new BlockRendererCollection(s => new EntityPartRenderer(s, 0))

  private val chunkRenderUpdater: ChunkRenderUpdater = new ChunkRenderUpdater(
    coords => {
      val r = chunkRenderers.get(coords)
      r.foreach(chunkHandler.updateChunk)
      r.isDefined
    },
    renderDistance
  )

  def tick(camera: Camera): Unit = {
    chunkRenderUpdater.update(camera)
  }

  def render(): Unit = {
    mainFrameBuffer.bind()

    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)

    skyShader.enable()
    skyRenderer.render()

    // World content
    chunkHandler.render()
    renderEntities()

    if (selectedSide.isDefined) {
      selectedBlockShader.enable()
      selectedBlockRenderer.render()
    }

    mainFrameBuffer.unbind()
    GL11.glViewport(0, 0, window.framebufferSize.x, window.framebufferSize.y)

    GL13.glActiveTexture(GL13.GL_TEXTURE0)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainColorTexture)
    GL13.glActiveTexture(GL13.GL_TEXTURE1)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainDepthTexture)

    worldCombinerShader.enable()
    worldCombinerRenderer.render()

    GL13.glActiveTexture(GL13.GL_TEXTURE1)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    GL13.glActiveTexture(GL13.GL_TEXTURE0)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
  }

  override def onChunkAdded(chunk: Chunk): Unit = {
    val renderer = new ChunkRendererImpl(chunk, world)
    chunkRenderers(chunk.coords) = renderer
//    chunkHandler.addChunk(renderer)
    chunk.addEventListener(chunkRenderUpdater)
  }
  override def onChunkRemoved(chunk: Chunk): Unit = {
    chunkRenderers
      .remove(chunk.coords)
      .foreach(renderer => {
        chunkHandler.removeChunk(renderer)
        renderer.unload()
      })
    chunk.removeEventListener(chunkRenderUpdater)
  }

  def framebufferResized(width: Int, height: Int): Unit = {
    mainFrameBuffer.unload()

    mainColorTexture = makeMainColorTexture(width, height)
    mainDepthTexture = makeMainDepthTexture(width, height)
    mainFrameBuffer = makeMainFrameBuffer(mainColorTexture, mainDepthTexture, width, height)
  }

  def unload(): Unit = {
    skyVAO.free()
    selectedBlockVAO.free()
    worldCombinerVAO.free()

    entityRenderers.unload()
    chunkHandler.unload()
    mainFrameBuffer.unload()

    GL11.glDeleteTextures(mainColorTexture)
    GL11.glDeleteTextures(mainDepthTexture)
  }

  private def renderEntities(): Unit = {
    for (side <- 0 until 8) {
      val sh = if (side < 2) entityShader else entitySideShader
      sh.enable()
      sh.setUniform1i("side", side)

      val entityDataList: mutable.Buffer[EntityDataForShader] = ListBuffer.empty
      chunkRenderers.values.foreach(_.appendEntityRenderData(side, entityDataList += _))
      for ((model, partLists) <- entityDataList.groupBy(_.model)) {
        val data = partLists.flatMap(_.parts)

        entityRenderers.updateContent(side, data.size) { buf =>
          data.foreach(_.fill(buf))
        }
        model.texture.bind()
        sh.setUniform1i("texSize", model.texSize)
        entityRenderers.renderBlockSide(side)
      }
    }
  }

  private def makeSelectedBlockVAO: VAO = {
    def expandFn(v: CylCoords): Seq[Float] =
      Seq(v.x * 1.0025, (v.y - 0.25) * 1.0025 + 0.25, v.z * 1.0025).map(_.toFloat)

    def fn(s: Int): Seq[Float] = BlockState.getVertices(s + 2).flatMap(expandFn)

    val vertexData =
      Seq(0, 2, 4).flatMap(fn) ++ expandFn(BlockState.vertices.head) ++ Seq(1, 3, 5).flatMap(fn)

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
  }

  private def makeSkyVAO: VAO = {
    new VAOBuilder(4)
      .addVBO(
        VBOBuilder(4)
          .floats(0, 2)
          .create()
          .fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
      )
      .create()
  }

  private def makeMainColorTexture(framebufferWidth: Int, framebufferHeight: Int): Int = {
    val texID = GL11.glGenTextures()

    TextureSingle.unbind()
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
    GL11.glTexImage2D(
      GL11.GL_TEXTURE_2D,
      0,
      GL11.GL_RGBA,
      framebufferWidth,
      framebufferHeight,
      0,
      GL11.GL_RGBA,
      GL11.GL_UNSIGNED_BYTE,
      null.asInstanceOf[ByteBuffer]
    )
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)

    texID
  }

  private def makeMainDepthTexture(framebufferWidth: Int, framebufferHeight: Int): Int = {
    val texID = GL11.glGenTextures()

    TextureSingle.unbind()
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID)
    GL11.glTexImage2D(
      GL11.GL_TEXTURE_2D,
      0,
      GL14.GL_DEPTH_COMPONENT32,
      framebufferWidth,
      framebufferHeight,
      0,
      GL11.GL_DEPTH_COMPONENT,
      GL11.GL_FLOAT,
      null.asInstanceOf[FloatBuffer]
    )
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)

    texID
  }

  private def makeMainFrameBuffer(
      colorTexture: Int,
      depthTexture: Int,
      framebufferWidth: Int,
      framebufferHeight: Int
  ): FrameBuffer = {
    val fb = new FrameBuffer(framebufferWidth, framebufferHeight)
    fb.bind()
    GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0)
    GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, colorTexture, 0)
    GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, depthTexture, 0)

    fb
  }

  private def updateSelectedBlockVAO(coords: BlockRelWorld) = {
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
  }
}
