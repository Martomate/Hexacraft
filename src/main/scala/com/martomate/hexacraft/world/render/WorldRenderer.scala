package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer.*
import com.martomate.hexacraft.util.{CylinderSize, OpenGL, RevokeTrackerFn}
import com.martomate.hexacraft.world.{BlocksInWorld, LightPropagator, World}
import com.martomate.hexacraft.world.block.{Blocks, BlockState}
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.chunk.storage.LocalBlockState
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelChunk, BlockRelWorld, ChunkRelWorld}

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import org.joml.Vector2ic
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class WorldRenderer(world: BlocksInWorld, initialFramebufferSize: Vector2ic)(using CylinderSize, Blocks):
  private val skyShader = Shader.get(Shaders.ShaderNames.Sky).get
  private val entityShader = Shader.get(Shaders.ShaderNames.Entity).get
  private val entitySideShader = Shader.get(Shaders.ShaderNames.EntitySide).get
  private val selectedBlockShader = Shader.get(Shaders.ShaderNames.SelectedBlock).get
  private val worldCombinerShader = Shader.get(Shaders.ShaderNames.WorldCombiner).get

  worldCombinerShader.setUniform1i("worldColorTexture", 0)
  worldCombinerShader.setUniform1i("worldDepthTexture", 1)

  private val chunkHandler: ChunkRenderHandler = new ChunkRenderHandler

  private val lightPropagator = new LightPropagator(world)

  private val skyVAO: VAO = Helpers.makeSkyVAO
  private val skyRenderer = new Renderer(skyVAO, OpenGL.PrimitiveMode.TriangleStrip) with NoDepthTest

  private val worldCombinerVAO: VAO = Helpers.makeSkyVAO
  private val worldCombinerRenderer = new Renderer(worldCombinerVAO, OpenGL.PrimitiveMode.TriangleStrip)
    with NoDepthTest

  private val selectedBlockVAO: VAO = Helpers.makeSelectedBlockVAO
  private val selectedBlockRenderer = new InstancedRenderer(selectedBlockVAO, OpenGL.PrimitiveMode.LineStrip)

  private var mainFrameBuffer = MainFrameBuffer.fromSize(initialFramebufferSize.x, initialFramebufferSize.y)

  private var currentlySelectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = None

  private val chunksToRender: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val entityRenderers: BlockRendererCollection = new BlockRendererCollection(s => EntityPartRenderer.forSide(s))

  private val chunkRenderUpdater: ChunkRenderUpdater = new ChunkRenderUpdater

  private val chunkEventTrackerRevokeFns = mutable.Map.empty[ChunkRelWorld, RevokeTrackerFn]

  private def updateChunkIfPresent(coords: ChunkRelWorld) =
    world.getChunk(coords) match
      case Some(ch) =>
        ch.initLightingIfNeeded(lightPropagator)

        val renderData: ChunkRenderData =
          if ch.blocks.numBlocks == 0
          then ChunkRenderData.empty
          else ChunkRenderDataFactory.makeChunkRenderData(ch.coords, ch.blocks.allBlocks, world)

        chunkHandler.setChunkRenderData(coords, renderData)
        true
      case None =>
        false

  def tick(camera: Camera, renderDistance: Double): Unit =
    chunkRenderUpdater.update(camera, renderDistance, updateChunkIfPresent)

  def onTotalSizeChanged(totalSize: Int): Unit =
    chunkHandler.onTotalSizeChanged(totalSize)

    entityShader.setUniform1i("totalSize", totalSize)
    entitySideShader.setUniform1i("totalSize", totalSize)
    selectedBlockShader.setUniform1i("totalSize", totalSize)

  def onProjMatrixChanged(camera: Camera): Unit =
    chunkHandler.onProjMatrixChanged(camera)

    camera.setProjMatrix(entityShader)
    camera.setProjMatrix(entitySideShader)
    camera.setProjMatrix(selectedBlockShader)

    skyShader.setUniformMat4("invProjMatr", camera.proj.invMatrix)

    worldCombinerShader.setUniform1f("nearPlane", camera.proj.near)
    worldCombinerShader.setUniform1f("farPlane", camera.proj.far)

  def render(camera: Camera, sun: Vector3f, selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])]): Unit =
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

    OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)

    skyShader.setUniformMat4("invViewMatr", camera.view.invMatrix)
    skyShader.setUniform3f("sun", sun.x, sun.y, sun.z)
    skyShader.enable()
    skyRenderer.render()

    // World content
    chunkHandler.render(camera, sun)
    renderEntities(camera, sun)

    if selectedBlockAndSide.flatMap(_._2).isDefined then
      camera.updateUniforms(selectedBlockShader)
      selectedBlockShader.enable()
      selectedBlockRenderer.render()

    mainFrameBuffer.unbind()
    OpenGL.glViewport(0, 0, mainFrameBuffer.frameBuffer.width, mainFrameBuffer.frameBuffer.height)

    // Step 2: Render the FrameBuffer to the screen (one could add post processing here in the future)
    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(0))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.colorTexture)
    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(1))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.depthTexture)

    worldCombinerShader.enable()
    worldCombinerRenderer.render()

    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(1))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)
    OpenGL.glActiveTexture(OpenGL.TextureSlot.ofSlot(0))
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, OpenGL.TextureId.none)

  def onWorldEvent(event: World.Event): Unit =
    event match
      case World.Event.ChunkAdded(chunk) =>
        chunksToRender.add(chunk.coords)

        val revoke = chunk.trackEvents(chunkRenderUpdater.onChunkEvent _)
        chunkEventTrackerRevokeFns += chunk.coords -> revoke
      case World.Event.ChunkRemoved(chunk) =>
        chunksToRender.remove(chunk.coords)
        chunkHandler.clearChunkRenderData(chunk.coords)

        val revoke = chunkEventTrackerRevokeFns(chunk.coords)
        revoke()

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

  private def renderEntities(camera: Camera, sun: Vector3f): Unit =
    camera.updateUniforms(entityShader)
    camera.updateUniforms(entitySideShader)
    entityShader.setUniform3f("sun", sun.x, sun.y, sun.z)
    entitySideShader.setUniform3f("sun", sun.x, sun.y, sun.z)

    for side <- 0 until 8 do
      val sh = if side < 2 then entityShader else entitySideShader
      sh.enable()
      sh.setUniform1i("side", side)

      val entityDataList: mutable.ArrayBuffer[EntityDataForShader] = ArrayBuffer.empty
      for
        c <- chunksToRender
        ch <- world.getChunk(c)
        if ch.entities.count > 0
      do entityDataList ++= EntityRenderDataFactory.getEntityRenderData(ch.entities, side, world)

      for (texture, partLists) <- entityDataList.groupBy(_.model.texture) do
        val data = partLists.flatMap(_.parts)

        entityRenderers.updateContent(side, data.size) { buf =>
          data.foreach(_.fill(buf))
        }
        texture.bind()
        sh.setUniform1i("texSize", texture.width)
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
          VBOBuilder()
            .floats(0, 3)
            .create(25)
            .fillFloats(0, vertexData)
        )
        .addVBO(
          VBOBuilder()
            .ints(1, 3)
            .floats(2, 3)
            .floats(3, 1)
            .create(1, OpenGL.VboUsage.DynamicDraw, 1)
        )
        .create()

    def makeSkyVAO: VAO =
      new VAOBuilder(4)
        .addVBO(
          VBOBuilder()
            .floats(0, 2)
            .create(4)
            .fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
        )
        .create()
