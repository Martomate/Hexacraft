package hexacraft.world.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, InstancedRenderer, Renderer, VAO}
import hexacraft.util.RevokeTrackerFn
import hexacraft.world.{BlocksInWorld, CylinderSize, LightPropagator, World}
import hexacraft.world.block.{BlockSpecRegistry, BlockState}
import hexacraft.world.camera.Camera
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.Entity

import org.joml.{Vector2ic, Vector3f}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class WorldRenderer(world: BlocksInWorld, initialFramebufferSize: Vector2ic)(using CylinderSize, BlockSpecRegistry):
  private val skyShader = new SkyShader()
  private val entityShader = new EntityShader(isSide = false)
  private val entitySideShader = new EntityShader(isSide = true)
  private val selectedBlockShader = new SelectedBlockShader()
  private val worldCombinerShader = new WorldCombinerShader()

  private val chunkHandler: ChunkRenderHandler = new ChunkRenderHandler

  private val lightPropagator = new LightPropagator(world)

  private val skyVao: VAO = SkyVao.create
  private val skyRenderer =
    new Renderer(OpenGL.PrimitiveMode.TriangleStrip, GpuState.of(OpenGL.State.DepthTest -> false))

  private val worldCombinerVao: VAO = WorldCombinerVao.create
  private val worldCombinerRenderer =
    new Renderer(OpenGL.PrimitiveMode.TriangleStrip, GpuState.of(OpenGL.State.DepthTest -> false))

  private val selectedBlockVao = SelectedBlockVao.create
  private val selectedBlockRenderer = new InstancedRenderer(OpenGL.PrimitiveMode.LineStrip)

  private var mainFrameBuffer = MainFrameBuffer.fromSize(initialFramebufferSize.x, initialFramebufferSize.y)

  private var currentlySelectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])] = None

  private val chunksToRender: mutable.Set[ChunkRelWorld] = mutable.HashSet.empty
  private val entityRenderers = for s <- 0 until 8 yield BlockRenderer(EntityPartVao.forSide(s), GpuState())

  private val chunkRenderUpdater: ChunkRenderUpdater = new ChunkRenderUpdater

  private val chunkEventTrackerRevokeFns = mutable.Map.empty[ChunkRelWorld, RevokeTrackerFn]

  private val players = ArrayBuffer.empty[Entity]
  def addPlayer(player: Entity): Unit = players += player
  def removePlayer(player: Entity): Unit = players -= player

  def regularChunkBufferFragmentation: IndexedSeq[Float] = chunkHandler.regularChunkBufferFragmentation
  def transmissiveChunkBufferFragmentation: IndexedSeq[Float] = chunkHandler.transmissiveChunkBufferFragmentation

  private def updateChunkIfPresent(coords: ChunkRelWorld) =
    val chunkOpt = world.getChunk(coords)
    for chunk <- chunkOpt do updateChunkData(chunk)
    chunkOpt.isDefined

  private def updateChunkData(ch: Chunk): Unit =
    ch.initLightingIfNeeded(lightPropagator)

    val (opaqueBlocks, transmissiveBlocks) =
      if ch.blocks.isEmpty
      then (ChunkRenderData.empty, ChunkRenderData.empty)
      else
        (
          ChunkRenderDataFactory.makeChunkRenderData(ch.coords, ch.blocks, world, false),
          ChunkRenderDataFactory.makeChunkRenderData(ch.coords, ch.blocks, world, true)
        )

    chunkHandler.setChunkRenderData(ch.coords, opaqueBlocks, transmissiveBlocks)

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

  def render(
      camera: Camera,
      sun: Vector3f,
      selectedBlockAndSide: Option[(BlockState, BlockRelWorld, Option[Int])]
  ): Unit =
    // Update the 'selectedBlockVAO' if needed
    if currentlySelectedBlockAndSide != selectedBlockAndSide
    then
      currentlySelectedBlockAndSide = selectedBlockAndSide
      selectedBlockAndSide match
        case Some((state, coords, Some(_))) => selectedBlockVao.setSelectedBlock(coords, state)
        case _                              =>

    // Step 1: Render everything to a FrameBuffer
    mainFrameBuffer.bind()

    OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)

    skyShader.setInverseViewMatrix(camera.view.invMatrix)
    skyShader.setSunPosition(sun)
    skyShader.enable()
    skyRenderer.render(skyVao)

    // World content
    chunkHandler.render(camera, sun)
    renderEntities(camera, sun)

    if selectedBlockAndSide.flatMap(_._3).isDefined then
      selectedBlockShader.setViewMatrix(camera.view.matrix)
      selectedBlockShader.setCameraPosition(camera.position)
      selectedBlockShader.enable()
      selectedBlockRenderer.render(selectedBlockVao.vao, 1)

    mainFrameBuffer.unbind()
    OpenGL.glViewport(0, 0, mainFrameBuffer.frameBuffer.width, mainFrameBuffer.frameBuffer.height)

    // Step 2: Render the FrameBuffer to the screen (one could add post processing here in the future)
    OpenGL.glActiveTexture(worldCombinerShader.colorTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.colorTexture)
    OpenGL.glActiveTexture(worldCombinerShader.depthTextureSlot)
    OpenGL.glBindTexture(OpenGL.TextureTarget.Texture2D, mainFrameBuffer.depthTexture)

    worldCombinerShader.enable()
    worldCombinerRenderer.render(worldCombinerVao)

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
    skyVao.free()
    skyShader.free()
    selectedBlockVao.free()
    selectedBlockShader.free()
    worldCombinerVao.free()
    worldCombinerShader.free()
    entityShader.free()
    entitySideShader.free()

    for r <- entityRenderers do r.unload()
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
      do
        val entities = ch.entities
        if entities.nonEmpty then
          val data = EntityRenderDataFactory.getEntityRenderData(entities, side, world)
          entityDataList ++= data

      entityDataList ++= EntityRenderDataFactory.getEntityRenderData(players, side, world)

      for (texture, partLists) <- entityDataList.groupBy(_.model.texture) do
        val data = partLists.flatMap(_.parts)

        entityRenderers(side).setInstanceData(data.size): buf =>
          data.foreach(_.fill(buf))

        texture.bind()
        sh.setTextureSize(texture.width)
        entityRenderers(side).render()
