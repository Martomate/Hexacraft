package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.Camera
import com.martomate.hexacraft.block.BlockState
import com.martomate.hexacraft.renderer._
import com.martomate.hexacraft.resource.{Shader, TextureArray}
import com.martomate.hexacraft.world.coord.{BlockRelWorld, ChunkRelWorld, CylCoords}
import com.martomate.hexacraft.world.storage.{Chunk, ChunkAddedOrRemovedListener, World}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class WorldRenderer(world: World) extends ChunkAddedOrRemovedListener {
  private val blockShader = Shader.get("block").get
  private val blockSideShader = Shader.get("blockSide").get
  private val skyShader = Shader.get("sky").get
  private val selectedBlockShader = Shader.get("selectedBlock").get
  private val blockTexture = TextureArray.getTextureArray("blocks")

  private val renderingJobs = ArrayBuffer.empty[RenderingJob]

  private val skyVAO: VAO = new VAOBuilder(4).addVBO(VBO(4).floats(0, 2).create().fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))).create()
  private val skyRenderer = new Renderer(skyVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest

  private val selectedBlockVAO: VAO = new VAOBuilder(25)
    .addVBO(VBO(25).floats(0, 3).create().fillFloats(0, {
      val expandFn: CylCoords => Seq[Float] = v => Seq((v.x * 1.0025).toFloat, ((v.y - 0.25) * 1.0025 + 0.25).toFloat, (v.z * 1.0025).toFloat)
      val fn: Int => Seq[Float] = s => BlockState.getVertices(s + 2).flatMap(expandFn)
      Seq(0, 2, 4).flatMap(fn) ++ expandFn(BlockState.vertices.head) ++ Seq(1, 3, 5).flatMap(fn)
    }))
    .addVBO(VBO(1, divisor = 1).ints(1, 3).floats(2, 3).floats(3, 1).create()).create()
  private val selectedBlockRenderer = new InstancedRenderer(selectedBlockVAO, GL11.GL_LINE_STRIP)

  private var selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = None
  def getSelectedBlock: Option[BlockRelWorld] = selectedBlockAndSide.map(_._1)
  def getSelectedSide: Option[Int] = selectedBlockAndSide.flatMap(_._2)
  def getSelectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = selectedBlockAndSide

  def setSelectedBlockAndSide(blockAndSide: Option[(BlockRelWorld, Option[Int])]): Unit = {
    if (selectedBlockAndSide != blockAndSide) {
      selectedBlockAndSide = blockAndSide

      blockAndSide match {
        case Some((coords, Some(_))) =>
          val blockState = world.getBlock(coords)
          val buf = BufferUtils.createByteBuffer(7 * 4).putInt(coords.x).putInt(coords.y).putInt(coords.z).putFloat(0).putFloat(0).putFloat(0)
          buf.putFloat(blockState.blockType.blockHeight(blockState))
          buf.flip()
          selectedBlockVAO.vbos(1).fill(0, buf)
        case _ =>
      }
    }
  }

  world.addChunkAddedOrRemovedListener(this)
  private val chunkRenderers: mutable.Map[ChunkRelWorld, ChunkRenderer] = mutable.HashMap.empty

  private val chunkRenderUpdater: ChunkRenderUpdater = new ChunkRenderUpdater(chunkRenderers.get, world.renderDistance, world.size)

  def tick(camera: Camera): Unit = {
    chunkRenderUpdater.update(camera)
  }

  def render(camera: Camera): Unit = {
    skyShader.enable()
    skyRenderer.render()

    for (job <- renderingJobs) {
      job.setup()

      for (r <- chunkRenderers.values) {
        job(r)
      }
    }

    if (getSelectedSide.isDefined) {
      selectedBlockShader.enable()
      selectedBlockRenderer.render()
    }
  }

  for (side <- 0 until 8) {
    registerRenderingJob(RenderingJob(_.renderBlockSide(side), () => {
      blockTexture.bind()
      val sh = if (side < 2) blockShader else blockSideShader
      sh.enable()
      sh.setUniform1i("side", side)
    }))
  }

  def registerRenderingJob(job: RenderingJob): Unit = {
    renderingJobs += job
  }

  def unload(): Unit = {
    skyVAO.free()
    selectedBlockVAO.free()
  }

  override def onChunkAdded(chunk: Chunk): Unit = {
    chunkRenderers(chunk.coords) = new ChunkRenderer(chunk, world)
    chunk.addEventListener(chunkRenderUpdater)
  }
  override def onChunkRemoved(chunk: Chunk): Unit = {
    chunkRenderers.remove(chunk.coords).foreach(_.unload())
    chunk.removeEventListener(chunkRenderUpdater)
  }
}

case class RenderingJob(job: ChunkRenderer => Unit, setup: () => Unit) {
  def apply(r: ChunkRenderer): Unit = job(r)
}