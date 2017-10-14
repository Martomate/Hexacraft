package hexagon.world.render

import scala.collection.mutable.BitSet

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15

import hexagon.block.Block
import hexagon.renderer.InstancedRenderer
import hexagon.renderer.VAO
import hexagon.renderer.VAOBuilder
import hexagon.renderer.VBO
import hexagon.resource.Shader
import hexagon.resource.TextureArray
import hexagon.block.BlockState
import hexagon.world.storage.Chunk

class ChunkRenderer(chunk: Chunk) {
  private val blockRenderers = Seq.tabulate(2)(s => new BlockRenderer(s, 0))
  private val blockSideRenderers = Seq.tabulate(6)(s => new BlockRenderer(s + 2, 0))
  private val allBlockRenderers = blockRenderers ++ blockSideRenderers

  val blockShader = Shader.getShader("block").get
  val blockSideShader = Shader.getShader("blockSide").get
  val blockTexture = TextureArray.getTextureArray("blocks")

  // TODO: Make it so that if a block should be removed from a vbo it finds the index in the vbo and moves the last block to that index.
  // This would make the updating effort proportional to the number of blocks to change, 
  // and would not require a total recalculation of the content in the chunk. This is similar to the system in Hexagon.
  // If a block is changed (added or removed), that change has to be saved so that this renderer can do the update described above. This can be saved in a list.
  // This reduced system should NOT be used when the chunk has just been created.
  def updateContent(): Unit = {
    val blocks = chunk.blocks.allBlocks
    val sidesToRender = Seq.fill(8)(new BitSet(16 * 16 * 16))
    val sidesCount = new Array[Int](8)
    for (b <- blocks) {
      val c = b.coord.getBlockRelChunk
      for (s <- BlockState.neighborOffsets.indices) {
        if (b.neighbor(s, chunk).filter(_.blockType != Block.Air).isEmpty) {
          sidesToRender(s)(c.value) = true
          sidesCount(s) += 1
        }
      }
    }
    for (i <- 0 until 8) if (sidesCount(i) > allBlockRenderers(i).maxInstances) allBlockRenderers(i).resize(sidesCount(i))
    for (r <- allBlockRenderers) {
      r.instances = 0
      val buf = BufferUtils.createByteBuffer(sidesCount(r.side) * allBlockRenderers(r.side).vao.vbos(1).stride)
      for (block <- blocks) {
        if (sidesToRender(r.side)(block.coord.getBlockRelChunk.value)) {
          r.instances += 1
          Seq(block.coord.x, block.coord.y, block.coord.z, block.blockType.blockTex(r.side)).foreach(buf.putInt)
        }
      }
      buf.flip()
      r.vao.vbos(1).fill(0, buf)
    }
  }

  def renderBlocks(): Unit = {
    blockTexture.bind()
    for (r <- blockRenderers) {
      blockShader.setUniform1i("side", r.side)
      r.renderer.render(r.instances)
    }
  }

  def renderBlockSides(): Unit = {
    blockTexture.bind()
    for (r <- blockSideRenderers) {
      blockSideShader.setUniform1i("side", r.side)
      r.renderer.render(r.instances)
    }
  }

  def unload(): Unit = {
    allBlockRenderers.foreach(_.unload)
  }

  class BlockRenderer(val side: Int, init_maxInstances: Int) {
    private var _maxInstances = init_maxInstances
    def maxInstances: Int = _maxInstances

    val vao: VAO = new VAOBuilder(if (side < 2) 6 else 4, maxInstances)
      .addVBO(VBO(if (side < 2) 6 else 4, GL15.GL_STATIC_DRAW, 0).floats(0, 3).floats(1, 2).floats(2, 3).create().fillFloats(0, setupBlockVBO(side)))
      .addVBO(VBO(maxInstances, GL15.GL_DYNAMIC_DRAW, 1).ints(3, 3).ints(4, 1).create()).create()

    val renderer = new InstancedRenderer(vao, GL11.GL_TRIANGLE_STRIP)

    var instances = 0

    def resize(newMaxInstances: Int): Unit = {
      _maxInstances = newMaxInstances
      vao.vbos(1).resize(newMaxInstances)
    }

    private def setupBlockVBO(s: Int): Seq[Float] = {
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
}
