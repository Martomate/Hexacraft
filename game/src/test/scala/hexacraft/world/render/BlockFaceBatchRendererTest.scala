package hexacraft.world.render

import hexacraft.world.CylinderSize
import hexacraft.world.coord.ChunkRelWorld

import munit.FunSuite

import java.nio.ByteBuffer

class BlockFaceBatchRendererTest extends FunSuite {
  given CylinderSize = CylinderSize(4)

  test("it works for one chunk") {
    val buffer = LocalRenderBuffer(3 * 4)
    val bufferAllocator = LocalRenderBuffer.Allocator(_ => buffer)
    val bufferHandler = new BufferHandler(4, 3, bufferAllocator)
    val renderHandler = new BlockFaceBatchRenderer(bufferHandler)

    val coords = ChunkRelWorld(11, 12, 13)
    renderHandler.update(Nil, Seq(coords -> ByteBuffer.wrap(Array(1, 2, 3, 4, 5, 6))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](1, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0)
    )

    renderHandler.update(Nil, Seq(coords -> ByteBuffer.wrap(Array(9, 8, 7, 6, 5, 4))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](9, 8, 7, 6, 5, 4, 0, 0, 0, 0, 0, 0)
    )

    renderHandler.update(Seq(coords), Seq(coords -> ByteBuffer.wrap(Array(1, 2, 3, 4, 5, 6))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](1, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0)
    )

    renderHandler.unload()
    assertEquals(buffer.unloadCount, 1)
  }

  test("it works for two chunks") {
    val buffer = LocalRenderBuffer(3 * 4)
    val bufferAllocator = LocalRenderBuffer.Allocator(_ => buffer)
    val bufferHandler = new BufferHandler(4, 3, bufferAllocator)
    val renderHandler = new BlockFaceBatchRenderer(bufferHandler)

    val coords1 = ChunkRelWorld(11, 12, 13)
    val coords2 = ChunkRelWorld(23, 22, 21)

    renderHandler.update(Nil, Seq(coords1 -> ByteBuffer.wrap(Array(1, 1, 1, 1, 1, 1))))
    renderHandler.update(Nil, Seq(coords2 -> ByteBuffer.wrap(Array(2, 2, 2, 2, 2, 2))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2)
    )

    renderHandler.update(Nil, Seq(coords1 -> ByteBuffer.wrap(Array(9, 8, 7, 6, 5, 4))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](9, 8, 7, 6, 5, 4, 2, 2, 2, 2, 2, 2)
    )

    renderHandler.update(Seq(coords1), Nil)
    assertEquals(
      buffer.localBuffer.array().toSeq.take(6),
      Seq[Byte](2, 2, 2, 2, 2, 2)
    )

    renderHandler.update(Nil, Seq(coords1 -> ByteBuffer.wrap(Array(1, 2, 3, 4, 5, 6))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](2, 2, 2, 2, 2, 2, 1, 2, 3, 4, 5, 6)
    )

    renderHandler.unload()
    assertEquals(buffer.unloadCount, 1)
  }

  test("replace chunk data with fewer bytes") {
    val buffer = LocalRenderBuffer(3 * 4)
    val bufferAllocator = LocalRenderBuffer.Allocator(_ => buffer)
    val bufferHandler = new BufferHandler(4, 3, bufferAllocator)
    val renderHandler = new BlockFaceBatchRenderer(bufferHandler)

    val coords1 = ChunkRelWorld(11, 12, 13)
    val coords2 = ChunkRelWorld(23, 22, 21)

    renderHandler.update(Nil, Seq(coords1 -> ByteBuffer.wrap(Array(11, 12, 13, 14, 15, 16))))
    renderHandler.update(Nil, Seq(coords2 -> ByteBuffer.wrap(Array(21, 22, 23, 24, 25, 26))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](11, 12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 26)
    )

    renderHandler.update(Nil, Seq(coords1 -> ByteBuffer.wrap(Array(19, 18, 17))))
    assertEquals(
      buffer.localBuffer.array().toSeq.take(9),
      Seq[Byte](19, 18, 17, 24, 25, 26, 21, 22, 23)
    )

    renderHandler.update(Nil, Seq(coords2 -> ByteBuffer.wrap(Array(29, 28, 27))))
    assertEquals(
      buffer.localBuffer.array().toSeq.take(6),
      Seq[Byte](19, 18, 17, 29, 28, 27)
    )
  }

  test("replace chunk data with more bytes") {
    val buffer = LocalRenderBuffer(3 * 4)
    val bufferAllocator = LocalRenderBuffer.Allocator(_ => buffer)
    val bufferHandler = new BufferHandler(4, 3, bufferAllocator)
    val renderHandler = new BlockFaceBatchRenderer(bufferHandler)

    val coords1 = ChunkRelWorld(11, 12, 13)
    val coords2 = ChunkRelWorld(23, 22, 21)

    renderHandler.update(Nil, Seq(coords1 -> ByteBuffer.wrap(Array(11, 12, 13))))
    renderHandler.update(Nil, Seq(coords2 -> ByteBuffer.wrap(Array(21, 22, 23, 24, 25, 26))))
    assertEquals(
      buffer.localBuffer.array().toSeq.take(9),
      Seq[Byte](11, 12, 13, 21, 22, 23, 24, 25, 26)
    )

    renderHandler.update(Nil, Seq(coords1 -> ByteBuffer.wrap(Array(19, 18, 17, 16, 15, 14))))
    assertEquals(
      buffer.localBuffer.array().toSeq,
      Seq[Byte](19, 18, 17, 21, 22, 23, 24, 25, 26, 16, 15, 14)
    )

    renderHandler.update(Seq(coords1), Nil)
    assertEquals(
      buffer.localBuffer.array().toSeq.take(6),
      Seq[Byte](24, 25, 26, 21, 22, 23)
    )
  }
}
