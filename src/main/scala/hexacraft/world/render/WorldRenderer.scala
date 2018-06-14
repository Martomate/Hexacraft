package hexacraft.world.render

import java.util

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import hexacraft.Camera
import hexacraft.renderer.InstancedRenderer
import hexacraft.renderer.NoDepthTest
import hexacraft.renderer.Renderer
import hexacraft.renderer.VAO
import hexacraft.renderer.VAOBuilder
import hexacraft.renderer.VBO
import hexacraft.resource.{Shader, TextureArray}
import hexacraft.world.coord.{BlockRelChunk, BlockRelWorld, CylCoords}
import hexacraft.block.BlockState
import hexacraft.world.storage.{Chunk, ChunkAddedOrRemovedListener, World}

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

  def getBrightness(block: BlockRelWorld): Float = {
    if (block != null) world.getChunk(block.getChunkRelWorld).map(_.renderer.getBrightness(block.getBlockRelChunk)).getOrElse(1.0f)
    else 1.0f
  }

  world.addChunkAddedOrRemovedListener(this)
  private val chunks: ArrayBuffer[Chunk] = ArrayBuffer.empty

  def render(camera: Camera): Unit = {
    skyShader.enable()
    skyRenderer.render()

    for (job <- renderingJobs) {
      job.setup()

      for (c <- chunks) {
        job(c.renderer)
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
    chunks += chunk
  }
  override def onChunkRemoved(chunk: Chunk): Unit = {
    chunks -= chunk
  }
}

case class RenderingJob(job: ChunkRenderer => Unit, setup: () => Unit) {
  def apply(r: ChunkRenderer): Unit = job(r)
}