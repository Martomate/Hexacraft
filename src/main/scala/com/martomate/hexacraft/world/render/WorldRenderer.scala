package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.renderer._
import com.martomate.hexacraft.resource.{Shader, TextureArray}
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.chunk.{ChunkAddedOrRemovedListener, IChunk}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.render.selector._
import com.martomate.hexacraft.world.worldlike.IWorld
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class WorldRenderer(world: IWorld) extends ChunkAddedOrRemovedListener {
  import world.size.impl

  private val entityShader = Shader.get("entity").get
  private val entitySideShader = Shader.get("entitySide").get
  private val skyShader = Shader.get("sky").get
  private val selectedBlockShader = Shader.get("selectedBlock").get

  private val chunkHandler: ChunkRenderHandler = new ChunkRenderHandler
  private val chunkRenderSelector: ChunkRenderSelector = new ChunkRenderSelectorIdentity(chunkHandler)

  private val skyVAO: VAO = makeSkyVAO
  private val skyRenderer = new Renderer(skyVAO, GL11.GL_TRIANGLE_STRIP) with NoDepthTest

  private val selectedBlockVAO: VAO = makeSelectedBlockVAO
  private val selectedBlockRenderer = new InstancedRenderer(selectedBlockVAO, GL11.GL_LINE_STRIP)

  private var _selectedBlockAndSide: Option[(BlockRelWorld, Option[Int])] = None
  def selectedBlock: Option[BlockRelWorld] = _selectedBlockAndSide.map(_._1)
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

  world.addChunkAddedOrRemovedListener(this)
  private val chunkRenderers: mutable.Map[ChunkRelWorld, ChunkRenderer] = mutable.HashMap.empty
  private val entityRenderers: BlockRendererCollection[EntityPartRenderer] =
    new BlockRendererCollection(s => new EntityPartRenderer(s, 0))

  private val chunkRenderUpdater: ChunkRenderUpdater = new ChunkRenderUpdater(coords => {
    val r = chunkRenderers.get(coords)
    r.foreach(chunkRenderSelector.updateChunk)
    r.isDefined
  }, world.renderDistance)

  def tick(camera: Camera): Unit = {
    chunkRenderUpdater.update(camera)

    chunkRenderSelector.tick(CylCoords(camera.view.position))
  }

  def render(camera: Camera): Unit = {
    skyShader.enable()
    skyRenderer.render()

    chunkHandler.render()

    renderEntities()

    if (selectedSide.isDefined) {
      selectedBlockShader.enable()
      selectedBlockRenderer.render()
    }
  }

  override def onChunkAdded(chunk: IChunk): Unit = {
    val renderer = new ChunkRendererImpl(chunk, world)
    chunkRenderers(chunk.coords) = renderer
//    chunkHandler.addChunk(renderer)
    chunk.addEventListener(chunkRenderUpdater)
  }
  override def onChunkRemoved(chunk: IChunk): Unit = {
    chunkRenderers.remove(chunk.coords).foreach(renderer => {
      chunkRenderSelector.removeChunk(renderer)
      renderer.unload()
    })
    chunk.removeEventListener(chunkRenderUpdater)
  }

  def unload(): Unit = {
    skyVAO.free()
    selectedBlockVAO.free()
    entityRenderers.unload()
    chunkHandler.unload()
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
    new VAOBuilder(25)
      .addVBO(VBOBuilder(25).floats(0, 3).create().fillFloats(0, {
        val expandFn: CylCoords => Seq[Float] = v => Seq((v.x * 1.0025).toFloat, ((v.y - 0.25) * 1.0025 + 0.25).toFloat, (v.z * 1.0025).toFloat)
        val fn: Int => Seq[Float] = s => BlockState.getVertices(s + 2).flatMap(expandFn)
        Seq(0, 2, 4).flatMap(fn) ++ expandFn(BlockState.vertices.head) ++ Seq(1, 3, 5).flatMap(fn)
      }))
      .addVBO(VBOBuilder(1, divisor = 1).ints(1, 3).floats(2, 3).floats(3, 1).create()).create()
  }

  private def makeSkyVAO: VAO = {
    new VAOBuilder(4).addVBO(VBOBuilder(4).floats(0, 2).create().fillFloats(0, Seq(-1, -1, 1, -1, -1, 1, 1, 1))).create()
  }

  private def updateSelectedBlockVAO(coords: BlockRelWorld) = {
    val blockState = world.getBlock(coords)
    val buf = BufferUtils.createByteBuffer(7 * 4).putInt(coords.x).putInt(coords.y).putInt(coords.z).putFloat(0).putFloat(0).putFloat(0)
    buf.putFloat(blockState.blockType.blockHeight(blockState.metadata))
    buf.flip()
    selectedBlockVAO.vbos(1).fill(0, buf)
  }
}
