package hexacraft.client

import hexacraft.client.ClientWorld.WorldTickResult
import hexacraft.client.entity.ModelComponent
import hexacraft.client.render.*
import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, TextureSingle, VAO}
import hexacraft.shaders.*
import hexacraft.util.{Loop, NamedThreadFactory}
import hexacraft.world.*
import hexacraft.world.chunk.Chunk
import hexacraft.world.entity.Entity

import org.joml.{Matrix4f, Vector2i, Vector2ic, Vector3f, Vector4fc}
import org.lwjgl.BufferUtils

import java.util.concurrent.Executors
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

/** @param waterSurfaceColor the average color and alpha of the texture of the water surface */
class WorldRenderer(
    world: ClientWorld,
    initialFrameBufferSize: Vector2ic,
    terrainRenderer: TerrainRenderer,
    waterSurfaceColor: Vector4fc
)(using CylinderSize) {
  private val executorService = Executors.newFixedThreadPool(8, NamedThreadFactory("render"))
  given ExecutionContext = ExecutionContext.fromExecutor(executorService)

  private val skyShader = new SkyShader()
  private val entityShader = new EntityShader(isSide = false)
  private val entitySideShader = new EntityShader(isSide = true)
  private val shadowEntityShader = new EntityShader(isSide = false)
  private val shadowEntitySideShader = new EntityShader(isSide = true)
  private val selectedBlockShader = new SelectedBlockShader()
  private val worldCombinerShader = new WorldCombinerShader(ShadowCascades.splits.size)

  skyShader.setOcean(world.worldInfo.gen.generateOceans, waterSurfaceColor)

  private val skyVao: VAO = SkyShader.createVao()
  private val skyRenderer = SkyShader.createRenderer()

  private val worldCombinerVao: VAO = WorldCombinerShader.createVao()
  private val worldCombinerRenderer = WorldCombinerShader.createRenderer()

  private val selectedBlockVao = SelectedBlockShader.createVao()
  private val selectedBlockRenderer = SelectedBlockShader.createRenderer()

  private var mainFrameBuffer = MainFrameBuffer.fromSize(initialFrameBufferSize.x, initialFrameBufferSize.y)
  // Translucent things are kept separate so that the things behind them can be seen while combining them
  private var translucentFrameBuffer = MainFrameBuffer.withDepthTextureOf(mainFrameBuffer)
  private var nextFrameBufferSize: Option[Vector2ic] = None

  private val shadowMap = new ShadowMap(ShadowCascades.resolution, ShadowCascades.splits.size)
  private val identityMatrix = new Matrix4f()

  private var currentlySelectedBlockAndSide: Option[MousePickerResult] = None

  private val entityRenderers = for s <- 0 until 8 yield BlockRenderer(EntityShader.createVao(s), GpuState())

  private val players = ArrayBuffer.empty[Entity]

  def addPlayer(player: Entity): Unit = {
    players += player
  }

  def removePlayer(player: Entity): Unit = {
    players -= player
  }

  def regularChunkBufferFragmentation: IndexedSeq[Float] =
    terrainRenderer.regularChunkBufferFragmentation

  def transmissiveChunkBufferFragmentation: IndexedSeq[Float] =
    terrainRenderer.transmissiveChunkBufferFragmentation

  def renderQueueLength: Int =
    terrainRenderer.renderQueueLength

  def tick(camera: Camera, renderDistance: Double, worldTickResult: WorldTickResult): Unit = {
    terrainRenderer.tick(camera, renderDistance, worldTickResult)
  }

  def onTotalSizeChanged(totalSize: Int): Unit = {

    terrainRenderer.onTotalSizeChanged(totalSize)

    entityShader.setTotalSize(totalSize)
    entitySideShader.setTotalSize(totalSize)
    shadowEntityShader.setTotalSize(totalSize)
    shadowEntitySideShader.setTotalSize(totalSize)
    selectedBlockShader.setTotalSize(totalSize)

    worldCombinerShader.setTotalSize(totalSize)
    skyShader.setTotalSize(totalSize)
  }

  def onProjMatrixChanged(camera: Camera): Unit = {

    terrainRenderer.onProjMatrixChanged(camera)

    entityShader.setProjectionMatrix(camera.proj.matrix)
    entitySideShader.setProjectionMatrix(camera.proj.matrix)
    selectedBlockShader.setProjectionMatrix(camera.proj.matrix)

    skyShader.setInverseProjectionMatrix(camera.proj.invMatrix)

    worldCombinerShader.setClipPlanes(camera.proj.near, camera.proj.far)
  }

  private def replaceFrameBufferIfNeeded(): Unit = {
    nextFrameBufferSize match {
      case Some(size) =>
        nextFrameBufferSize = None

        val newFrameBuffer = MainFrameBuffer.fromSize(size.x, size.y)
        val newTranslucentFrameBuffer = MainFrameBuffer.withDepthTextureOf(newFrameBuffer)
        translucentFrameBuffer.unload()
        mainFrameBuffer.unload()
        mainFrameBuffer = newFrameBuffer
        translucentFrameBuffer = newTranslucentFrameBuffer
      case None =>
    }
  }

  private def updateSelectedBlockVao(selectedBlockAndSide: Option[MousePickerResult]) = {
    if currentlySelectedBlockAndSide != selectedBlockAndSide then {
      currentlySelectedBlockAndSide = selectedBlockAndSide

      selectedBlockAndSide match {
        case Some(MousePickerResult(state, coords, Some(_))) =>
          val buf = BufferUtils.createByteBuffer(7 * 4)
          SelectedBlockShader.InstanceData(coords, state).fill(buf)
          buf.flip()
          selectedBlockVao.vbos(1).fill(0, buf)
        case _ =>
      }
    }
  }

  /** @param waterSurfaceHeight the height of the surface of the water that the camera is in (or right above), if any
    * @param waterEffects if the water fog and the distant ocean should be drawn. They assume that the eye is at the
    *                     camera position, which is not the case in free fly mode.
    */
  def render(
      camera: Camera,
      sun: Vector3f,
      selectedBlockAndSide: Option[MousePickerResult],
      waterSurfaceHeight: Option[Double],
      waterEffects: Boolean
  ): Unit = {
    val viewportSize = mainFrameBuffer.size

    replaceFrameBufferIfNeeded()
    updateSelectedBlockVao(selectedBlockAndSide)
    worldCombinerShader.setWaterEffectsEnabled(waterEffects)
    skyShader.setWaterEffectsEnabled(waterEffects)
    updateWaterSurface(camera, if waterEffects then waterSurfaceHeight else None)
    val eyeUnderWater = waterEffects && waterSurfaceHeight.exists(h => h > camera.position.y)

    val entityRenderData = collectEntityRenderData()

    // Step 0: Render the shadow casters from the point of view of the sun
    val shadowCascades = ShadowCascades.compute(camera.view.invMatrix, camera.proj.matrix, camera.position, sun)
    renderShadowMap(camera, shadowCascades, entityRenderData)

    // Step 1: Render all opaque things to a FrameBuffer
    mainFrameBuffer.bind()
    OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)

    // World content
    terrainRenderer.render(camera, sun, true, eyeUnderWater)

    entityShader.setViewMatrix(camera.view.matrix)
    entityShader.setCameraPosition(camera.position)
    entitySideShader.setViewMatrix(camera.view.matrix)
    entitySideShader.setCameraPosition(camera.position)
    renderEntities(entityRenderData, entityShader, entitySideShader)

    if selectedBlockAndSide.flatMap(_.side).isDefined then {
      renderSelectedBlock(camera)
    }

    mainFrameBuffer.unbind()

    // Step 2: Render all translucent things to another FrameBuffer (using the depth of the opaque things)
    translucentFrameBuffer.bind()
    OpenGL.glClear(OpenGL.ClearMask.colorBuffer)
    OpenGL.glDepthMask(false)

    terrainRenderer.render(camera, sun, false, eyeUnderWater)

    OpenGL.glDepthMask(true)
    translucentFrameBuffer.unbind()

    // Step 3: Render everything to the screen (one could add post processing here in the future)
    OpenGL.glViewport(0, 0, viewportSize.x, viewportSize.y)

    OpenGL.glClear(OpenGL.ClearMask.colorBuffer | OpenGL.ClearMask.depthBuffer)

    renderSky(camera, sun)
    renderFrameBuffers(sun, shadowCascades)
  }

  private def renderShadowMap(
      camera: Camera,
      cascades: IndexedSeq[ShadowCascades.Cascade],
      entityRenderData: Iterable[(String, IndexedSeq[EntityRenderData])]
  ): Unit = {
    // Rendering the back faces (seen from the sun) avoids most self-shadowing artifacts ("shadow acne") on lit faces,
    // and depth clamping makes sure that shadow casters between the sun and the near plane are not clipped away
    OpenGL.glCullFace(OpenGL.CullFaceMode.Front)
    OpenGL.glEnable(OpenGL.State.DepthClamp)

    for (cascade, i) <- cascades.zipWithIndex do {
      shadowMap.bind(i)
      OpenGL.glClear(OpenGL.ClearMask.depthBuffer)

      terrainRenderer.renderShadowCasters(camera, cascade.matrix)

      for sh <- Seq(shadowEntityShader, shadowEntitySideShader) do {
        sh.setProjectionMatrix(cascade.matrix)
        sh.setViewMatrix(identityMatrix)
        sh.setCameraPosition(camera.position)
      }
      renderEntities(entityRenderData, shadowEntityShader, shadowEntitySideShader)
    }

    shadowMap.unbind()

    OpenGL.glDisable(OpenGL.State.DepthClamp)
    OpenGL.glCullFace(OpenGL.CullFaceMode.Back)
  }

  private def updateWaterSurface(camera: Camera, waterSurfaceHeight: Option[Double]): Unit = {
    val (surfaceAboveEye, strength) = waterSurfaceHeight match {
      case Some(h) =>
        val surfaceAboveEye = h - camera.position.y
        (surfaceAboveEye.toFloat, 1f)
      case None =>
        (0f, 0f)
    }

    worldCombinerShader.setWaterSurface(surfaceAboveEye, strength)
    skyShader.setWaterSurface(surfaceAboveEye, strength)

    val seaLevel = WorldGenSettings.seaLevel * 0.5 // in CylCoords
    skyShader.setSeaLevelAboveEye((seaLevel - camera.position.y).toFloat)
  }

  private def renderFrameBuffers(sun: Vector3f, shadowCascades: IndexedSeq[ShadowCascades.Cascade]): Unit = {
    worldCombinerShader.bindTextures(
      positionTexture = mainFrameBuffer.positionTexture,
      normalTexture = mainFrameBuffer.normalTexture,
      colorTexture = mainFrameBuffer.colorTexture,
      depthTexture = mainFrameBuffer.depthTexture,
      translucentPositionTexture = translucentFrameBuffer.positionTexture,
      translucentNormalTexture = translucentFrameBuffer.normalTexture,
      translucentColorTexture = translucentFrameBuffer.colorTexture,
      shadowMap = shadowMap.texture
    )
    worldCombinerShader.enable()
    worldCombinerShader.setSunPosition(sun)
    for (c, i) <- shadowCascades.zipWithIndex do {
      worldCombinerShader.setShadowCascade(i, c.matrix, c.texelSize)
    }
    worldCombinerRenderer.render(worldCombinerVao, worldCombinerVao.maxCount)
    worldCombinerShader.unbindTextures()
  }

  private def renderSky(camera: Camera, sun: Vector3f): Unit = {
    skyShader.setInverseViewMatrix(camera.view.invMatrix)
    skyShader.setSunPosition(sun)
    skyShader.enable()
    skyRenderer.render(skyVao, skyVao.maxCount)
  }

  private def renderSelectedBlock(camera: Camera): Unit = {
    selectedBlockShader.setViewMatrix(camera.view.matrix)
    selectedBlockShader.setCameraPosition(camera.position)
    selectedBlockShader.enable()
    selectedBlockRenderer.render(selectedBlockVao, 1)
  }

  def frameBufferResized(width: Int, height: Int): Unit = {
    nextFrameBufferSize = Some(Vector2i(width, height))
  }

  /** @return the render data of all entities, grouped by texture name */
  private def collectEntityRenderData(): Iterable[(String, IndexedSeq[EntityRenderData])] = {
    val allEntities = mutable.ArrayBuffer.empty[Entity]
    world.foreachEntity(allEntities += _)
    allEntities ++= players

    allEntities
      .groupBy {
        _.accessComponent { case c: ModelComponent => c.skin.textureName }
      }
      .collect { case (Some(t), es) =>
        (t, EntityRenderData.fromEntities(es, world))
      }
  }

  private def renderEntities(
      entityRenderDataPerModel: Iterable[(String, IndexedSeq[EntityRenderData])],
      topBottomShader: EntityShader,
      sideShader: EntityShader
  ): Unit = {
    Loop.rangeUntil(0, 8) { side =>
      val sh = if side < 2 then topBottomShader else sideShader
      sh.enable()
      sh.setSide(side)

      Loop.iterate(entityRenderDataPerModel.iterator) { case (textureName, data) =>
        entityRenderers(side).setInstanceData(data.size) { buf =>
          Loop.array(data)(_.getInstanceData(side).fill(buf))
        }

        val texture = TextureSingle.getTexture("textures/entities/" + textureName)

        texture.bind()
        sh.setTextureSize(texture.width)
        entityRenderers(side).render()
      }
    }
  }

  def unload(): Unit = {
    skyVao.free()
    skyShader.free()
    selectedBlockVao.free()
    selectedBlockShader.free()
    worldCombinerVao.free()
    worldCombinerShader.free()
    entityShader.free()
    entitySideShader.free()
    shadowEntityShader.free()
    shadowEntitySideShader.free()

    for r <- entityRenderers do {
      r.unload()
    }

    terrainRenderer.unload()

    translucentFrameBuffer.unload()
    mainFrameBuffer.unload()
    shadowMap.unload()

    executorService.shutdown()
  }
}
