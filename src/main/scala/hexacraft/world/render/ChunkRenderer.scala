package hexacraft.world.render

import java.nio.ByteBuffer

import hexacraft.block.BlockState
import hexacraft.renderer._
import hexacraft.world.storage.Chunk
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.{GL11, GL15}

import scala.collection.mutable

class ChunkRenderer(chunk: Chunk) {
  private val blockRenderers = new BlockRendererCollection(s => new BlockRenderer(s, 0))

  // TODO: Make it so that if a block should be removed from a vbo it finds the index in the vbo and moves the last block to that index.
  // This would make the updating effort proportional to the number of blocks to change, 
  // and would not require a total recalculation of the content in the chunk. This is similar to the system in Hexagon.
  // If a block is changed (added or removed), that change has to be saved so that this renderer can do the update described above. This can be saved in a list.
  // This reduced system should NOT be used when the chunk has just been created.
  def updateContent(): Unit = {
    val blocks = chunk.blocks.allBlocks
    val sidesToRender = Seq.fill(8)(new mutable.BitSet(16 * 16 * 16))
    val sidesCount = new Array[Int](8)
    for (b <- blocks) {
      val c = b.coords.getBlockRelChunk
      for (s <- BlockState.neighborOffsets.indices) {
        if (b.neighbor(s, chunk).forall(bs => bs.blockType.isTransparent(bs, oppositeSide(s)))) {
          sidesToRender(s)(c.value) = true
          sidesCount(s) += 1
          if (s > 1 && b.blockType.isTransparent(b, s)) {
            sidesToRender(0)(c.value) = true
            sidesCount(0) += 1
          } // render the top side
        }
      }
    }
    for (side <- 0 until 8) {
      blockRenderers.updateContent(side, sidesCount(side), buf => {
        for (block <- blocks) {
          if (sidesToRender(side)(block.coords.getBlockRelChunk.value)) {
            buf.putInt(block.coords.x)
            buf.putInt(block.coords.y)
            buf.putInt(block.coords.z)
            buf.putInt(block.blockType.blockTex(side))
            buf.putFloat(block.blockType.blockHeight(block))
          }
        }
      })
    }
  }

  private def oppositeSide(s: Int) = {
    if (s < 2) 1 - s else (s - 2 + 3) % 3 + 2
  }

  def renderBlockSide(side: Int): Unit = blockRenderers.renderBlockSide(side)

  def unload(): Unit = {
    blockRenderers.unload()
  }

}

class BlockRendererCollection[T <: BlockRenderer](rendererFactory: Int => T) {
  val blockRenderers: Seq[T] = Seq.tabulate(2)(s => rendererFactory(s))
  val blockSideRenderers: Seq[T] = Seq.tabulate(6)(s => rendererFactory(s + 2))
  val allBlockRenderers: Seq[T] = blockRenderers ++ blockSideRenderers

  def renderBlockSide(side: Int): Unit = {
    val r = allBlockRenderers(side)
    r.renderer.render(r.instances)
  }

  def updateContent(side: Int, maxInstances: Int, dataFiller: ByteBuffer => Unit): Unit = {
    val buf = BufferUtils.createByteBuffer(maxInstances * allBlockRenderers(side).vao.vbos(1).stride)
    dataFiller(buf)
    val instances = buf.position() / allBlockRenderers(side).vao.vbos(1).stride
    if (instances > allBlockRenderers(side).maxInstances) allBlockRenderers(side).resize((instances * 1.1f).toInt)
    val r = allBlockRenderers(side)
    r.instances = instances
    buf.flip()
    r.vao.vbos(1).fill(0, buf)
  }

  def unload(): Unit = allBlockRenderers.foreach(_.unload())
}

class BlockRenderer(val side: Int, init_maxInstances: Int) {
  private var _maxInstances = init_maxInstances
  def maxInstances: Int = _maxInstances

  val vao: VAO = new VAOBuilder(if (side < 2) 6 else 4, maxInstances)
    .addVBO(VBO(if (side < 2) 6 else 4, GL15.GL_STATIC_DRAW).floats(0, 3).floats(1, 2).floats(2, 3).create().fillFloats(0, setupBlockVBO(side)))
    .addVBO(VBO(maxInstances, GL15.GL_DYNAMIC_DRAW, 1).ints(3, 3).ints(4, 1).floats(5, 1).create()).create()

  val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)

  var instances = 0

  def resize(newMaxInstances: Int): Unit = {
    _maxInstances = newMaxInstances
    vao.vbos(1).resize(newMaxInstances)
  }

  protected def setupBlockVBO(s: Int): Seq[Float] = {
    if (s < 2) {
      val ints = Seq(1, 2, 0, 3, 5, 4)

      (0 until 6).flatMap(i => {
        val v = {
          val a = ints(if (s == 0) i else 5 - i) * Math.PI / 3
          if (s == 0) -a else a
        }
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        Seq(x, 1 - s, z,
           (1 + (if (s == 0) -x else x)) / 2, (1 + z) / 2,
            0, 1 - 2 * s, 0)
      })
    } else {
      val nv = ((s - 1) % 6 - 0.5) * Math.PI / 3
      val nx = Math.cos(nv).toFloat
      val nz = Math.sin(nv).toFloat

      (0 until 4).flatMap(i => {
        val v = (s - 2 + i % 2) % 6 * Math.PI / 3
        val x = Math.cos(v).toFloat
        val z = Math.sin(v).toFloat
        Seq(x, 1 - i / 2, z,
            1 - i % 2, i / 2,
            nx, 0, nz)
      })
    }
  }

  def unload(): Unit = {
    vao.unload()
  }
}

class FlatBlockRenderer(val _side: Int, _init_maxInstances: Int) extends BlockRenderer(_side, _init_maxInstances) {
  override val vao: VAO = new VAOBuilder(if (side < 2) 6 else 4, maxInstances)
    .addVBO(VBO(if (side < 2) 6 else 4, GL15.GL_STATIC_DRAW).floats(0, 3).floats(1, 2).floats(2, 3).create().fillFloats(0, setupBlockVBO(side)))
    .addVBO(VBO(maxInstances, GL15.GL_DYNAMIC_DRAW, 1).floats(3, 2).ints(4, 1).floats(5, 1).create()).create()

  override val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP) with NoDepthTest
}