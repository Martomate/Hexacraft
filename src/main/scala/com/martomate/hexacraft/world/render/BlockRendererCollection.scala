package com.martomate.hexacraft.world.render

import java.nio.ByteBuffer
import org.lwjgl.BufferUtils

class BlockRendererCollection[T <: BlockRenderer](rendererFactory: Int => T) {
  val blockRenderers: IndexedSeq[T] = IndexedSeq.tabulate(2)(s => rendererFactory(s))
  val blockSideRenderers: IndexedSeq[T] = IndexedSeq.tabulate(6)(s => rendererFactory(s + 2))
  val allBlockRenderers: IndexedSeq[T] = blockRenderers ++ blockSideRenderers

  def renderBlockSide(side: Int): Unit = {
    val r = allBlockRenderers(side)
    r.renderer.render(r.instances)
  }

  def updateContent(side: Int, maxInstances: Int)(dataFiller: ByteBuffer => Unit): Unit = {
    val buf =
      BufferUtils.createByteBuffer(maxInstances * allBlockRenderers(side).vao.vbos(1).stride)
    dataFiller(buf)
    val instances = buf.position() / allBlockRenderers(side).vao.vbos(1).stride
    if (instances > allBlockRenderers(side).maxInstances)
      allBlockRenderers(side).resize((instances * 1.1f).toInt)
    val r = allBlockRenderers(side)
    r.instances = instances
    buf.flip()
    r.vao.vbos(1).fill(0, buf)
  }

  def unload(): Unit = allBlockRenderers.foreach(_.unload())
}
