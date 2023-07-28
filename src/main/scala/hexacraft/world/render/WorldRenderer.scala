package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{InstancedRenderer, NoDepthTest, Renderer, VAO}
import hexacraft.util.RevokeTrackerFn
import hexacraft.world.{BlocksInWorld, CylinderSize, LightPropagator, World}
import hexacraft.world.block.{Blocks, BlockState}
import hexacraft.world.camera.Camera
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.Entity

import org.joml.{Vector2ic, Vector3f}
import org.lwjgl.BufferUtils
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class WorldRenderer(world: BlocksInWorld, initialFramebufferSize: Vector2ic)(using CylinderSize, Blocks):
  private val skyShader = new SkyShader()
  private val entityShader = new EntityShader(isSide = false)
  private val entitySideShader = new EntityShader(isSide = true)
  private val selectedBlockShader = new SelectedBlockShader()
  private val worldCombinerShader = new WorldCombinerShader()

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

  private val players = ArrayBuffer.empty[Entity]
  def addPlayer(player: Entity): Unit = players += player
  def removePlayer(player: Entity): Unit = players -= player

  private def updateChunkIfPresent(coords: ChunkRelWorld) =
    val chunkOpt = world.getChunk(coords)
    for chunk <- chunkOpt do updateChunkData(chunk)
    chunkOpt.isDefined

  private def updateChunkData(ch: Chunk): Unit =
    ch.initLightingIfNeeded(lightPropagator)

    val renderData: ChunkRenderData =
      if ch.blocks.isEmpty
      then ChunkRenderData.empty
      else ChunkRenderDataFactory.makeChunkRenderData(ch.coords, ch.blocks, world)

    chunkHandler.setChunkRenderData(ch.coords, renderData)

  def tick(camera: Camera, renderDistance: Double): Unit =
    chunkRenderUpdater.update(camera, renderDistance, updateChunkIfPresent)

  def onTotalSizeChanged(totalSize: Int): Unit =
    chunkHandler.onTotalSizeChanged(totalSize)

    entityShader.setTotalSize(totalSize)
    entitySideShader.setTotalSize(totalSize)
    selectedBlockShader.setTotalSize(totalSize)

  def onProjMatrixChanged(camera: Camera): Unit =
    chunkHandler.onProjMatrixChanged(camera)

    entityShader.setProjectionMatrix(camera.proj.matrix)
    entitySideShader.setProjectionMatrix(camera.proj.matrix)
    selectedBlockShader.setProjectionMatrix(camera.proj.matrix)

    skyShader.setInverseProjectionMatrix(camera.proj.invMatrix)

    worldCombinerShader.setClipPlanes(camera.proj.near, camera.proj.far)

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

    skyShader.setInverseViewMatrix(camera.view.invMatrix)
    skyShader.setSunPosition(sun)
    skyShader.enable()
    skyRenderer.render()

    // World content
    chunkHandler.render(camera, sun)
    renderEntities(camera, sun)

    if selectedBlockAndSide.flatMap(_._2).isDefined then
      selectedBlockShader.setViewMatrix(camera.view.matrix)
      selectedBlockShader.setCameraPosition(camera.position)
      selectedBlockShader.enable()
      selectedBlockRenderer.render()

    mainFrameBuffer.unbind()
    OpenGL.glViewport(0, 0, mainFrameBuffer.frameBuffer.width, mainFrameBuffer.frameBuffer.height)

    // Step 2: Render the FrameBuffer to the screen (one could add post processing here in the future)
    OpenGL.glActiveTexture(worldCombinerShader.colorTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.colorTexture)
    OpenGL.glActiveTexture(worldCombinerShader.depthTextureSlot)
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
      case World.Event.ChunkRemoved(coords) =>
        chunksToRender.remove(coords)
        chunkHandler.clearChunkRenderData(coords)

        val revoke = chunkEventTrackerRevokeFns(coords)
        revoke()

  def framebufferResized(width: Int, height: Int): Unit =
    val newFrameBuffer = MainFrameBuffer.fromSize(width, height)
    mainFrameBuffer.unload()
    mainFrameBuffer = newFrameBuffer

  def unload(): Unit =
    skyVAO.free()
    selectedBlockVAO.free()
    selectedBlockShader.free()
    worldCombinerVAO.free()
    worldCombinerShader.free()
    skyShader.free()
    entityShader.free()
    entitySideShader.free()

    entityRenderers.unload()
    chunkHandler.unload()
    mainFrameBuffer.unload()

  private def renderEntities(camera: Camera, sun: Vector3f): Unit =
    entityShader.setViewMatrix(camera.view.matrix)
    entityShader.setCameraPosition(camera.position)
    entityShader.setSunPosition(sun)

    entitySideShader.setViewMatrix(camera.view.matrix)
    entitySideShader.setCameraPosition(camera.position)
    entitySideShader.setSunPosition(sun)

    for side <- 0 until 8 do
      val sh = if side < 2 then entityShader else entitySideShader
      sh.enable()
      sh.setSide(side)

      val entityDataList: mutable.ArrayBuffer[EntityDataForShader] = ArrayBuffer.empty
      for
        c <- chunksToRender
        ch <- world.getChunk(c)
      do entityDataList ++= EntityRenderDataFactory.getEntityRenderData(ch.entities, side, world)

      entityDataList ++= EntityRenderDataFactory.getEntityRenderData(players, side, world)

      for (texture, partLists) <- entityDataList.groupBy(_.model.texture) do
        val data = partLists.flatMap(_.parts)

        entityRenderers.updateContent(side, data.size) { buf =>
          data.foreach(_.fill(buf))
        }
        texture.bind()
        sh.setTextureSize(texture.width)
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

      VAO
        .builder()
        .addVertexVbo(25)(
          _.floats(0, 3),
          _.fillFloats(0, vertexData)
        )
        .addInstanceVbo(1, OpenGL.VboUsage.DynamicDraw)(
          _.ints(1, 3).floats(2, 3).floats(3, 1)
        )
        .finish(25)

    def makeSkyVAO: VAO =
      VAO
        .builder()
        .addVertexVbo(4)(
          _.floats(0, 2),
          _.fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))
        )
        .finish(4)
