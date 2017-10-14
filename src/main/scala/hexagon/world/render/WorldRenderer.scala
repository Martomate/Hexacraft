package hexagon.world.render

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

import hexagon.Camera
import hexagon.renderer.InstancedRenderer
import hexagon.renderer.NoDepthTest
import hexagon.renderer.Renderer
import hexagon.renderer.VAO
import hexagon.renderer.VAOBuilder
import hexagon.renderer.VBO
import hexagon.resource.Shader
import hexagon.world.coord.BlockRelWorld
import hexagon.world.coord.CylCoord
import hexagon.block.BlockState
import hexagon.world.storage.World

class WorldRenderer(world: World) {
  val blockShader = Shader.getShader("block").get
  val blockSideShader = Shader.getShader("blockSide").get
  val skyShader = Shader.getShader("sky").get
  val selectedBlockShader = Shader.getShader("selectedBlock").get

  private val skyVAO: VAO = new VAOBuilder(8).addVBO(VBO(4).floats(0, 2).create().fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))).create()
  private val skyRenderer = new Renderer(skyVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest

  private val selectedBlockVAO: VAO = new VAOBuilder(25)
    .addVBO(VBO(25).floats(0, 3).create().fillFloats(0, {
      val expandFn: CylCoord => Seq[Float] = v => Seq((v.x * 1.0025).toFloat, ((v.y - 0.25) * 1.0025 + 0.25).toFloat, (v.z * 1.0025).toFloat)
      val fn: Int => Seq[Float] = s => BlockState.getVertices(s + 2).flatMap(expandFn)
      Seq(0, 2, 4).flatMap(fn) ++ expandFn(BlockState.vertices(0)) ++ Seq(1, 3, 5).flatMap(fn)
    }))
    .addVBO(VBO(1, divisor = 1).ints(1, 3).floats(2, 3).create()).create()
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
          val buf = BufferUtils.createByteBuffer(6 * 4).putInt(coords.x).putInt(coords.y).putInt(coords.z).putFloat(0).putFloat(0).putFloat(0)
          buf.flip()
          selectedBlockVAO.vbos(1).fill(0, buf)
        case _ =>
      }
    }
  }

  def render(camera: Camera): Unit = {
    skyShader.enable()
    skyRenderer.render()

    blockShader.enable()
    world.columns.values.foreach(_.chunks.values.foreach(c => {
      c.renderer.foreach(_.renderBlocks())
    }))

    blockSideShader.enable()
    world.columns.values.foreach(_.chunks.values.foreach(c => {
      c.renderer.foreach(_.renderBlockSides())
    }))

    if (getSelectedSide != None) {
      selectedBlockShader.enable()
      selectedBlockRenderer.render()
    }
  }
}
