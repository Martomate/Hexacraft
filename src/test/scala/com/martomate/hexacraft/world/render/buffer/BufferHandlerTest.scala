package com.martomate.hexacraft.world.render.buffer

import com.martomate.hexacraft.world.render.buffer.{BufferHandler, RenderBuffer, RenderBufferFactory}
import com.martomate.hexacraft.world.render.segment.Segment

import java.nio.ByteBuffer
import munit.FunSuite

class BufferHandlerTest extends FunSuite {
  private val BytesPerInstance: Int = 3
  private val InstancesPerBuffer: Int = 4
  private val BufferSize: Int = BytesPerInstance * InstancesPerBuffer

  test("set should complain if the data has smaller length than the segment") {
    val handler = make(_ => null)
    intercept[IllegalArgumentException](handler.set(Segment(1, 3), simpleBuffer(3, 2)))
    intercept[IllegalArgumentException](handler.set(Segment(1, 300), simpleBuffer(3, 293)))
  }
  test("set should transfer the data into the correct buffer assuming no border is crossed") {
    val dest = new LocalRenderBuffer(BufferSize)
    val handler = make(maxInstances => dest ensuring (maxInstances == InstancesPerBuffer))
    handler.set(Segment(1, 3), rampFilledBuffer(1, 13))
    assertEquals(dest.localBuffer.array().toSeq.take(6), Seq(0, 1, 2, 3, 0, 0).map(_.toByte))
  }
  test("set should transfer the data into the correct buffers even if a border is crossed".ignore) {}
  test("set should transfer the data into the correct buffers even if several borders are crossed".ignore) {}

  test("copy should do nothing if len is 0".ignore) {}
  test("copy should move the data into the correct buffer assuming no border is crossed".ignore) {}
  test("copy should move the data into the correct buffers even if a border is crossed".ignore) {}
  test("copy should move the data into the correct buffers even if several borders are crossed".ignore) {}
  test("copy should be able to move a distance shorter than len if from < to".ignore) {}

  test("render should do nothing if length is 0".ignore) {}
  test("render should render the correct amount if no border is crossed".ignore) {}
  test("render should render the correct amount if a border is crossed".ignore) {}
  test("render should render the correct amount if several borders are crossed".ignore) {}

  test("unload should unload all buffers".ignore) {}

  def simpleBuffer(start: Int, len: Int): ByteBuffer = {
    val bf = ByteBuffer.allocate(start + len + 2)
    bf.position(start).limit(start + len)
    bf
  }

  def filledBuffer(start: Int, len: Int)(filler: Int => Byte): ByteBuffer = {
    val buf = simpleBuffer(start, len)
    for (i <- start until start + len)
      buf.put(filler(i))
    buf.position(start)
    buf
  }

  def rampFilledBuffer(start: Int, len: Int): ByteBuffer = filledBuffer(start, len)(_.toByte)

  def make[T <: RenderBuffer[T]](mkBuffer: Int => T) =
    new BufferHandler(4, new LocalFactory(mkBuffer))

  private class LocalFactory[T <: RenderBuffer[T]](mkBuffer: Int => T) extends RenderBufferFactory[T] {
    override def bytesPerInstance: Int = BytesPerInstance
    override def makeBuffer(maxInstances: Int): T = mkBuffer(maxInstances)
  }

  private class LocalRenderBuffer(size: Int) extends RenderBuffer[LocalRenderBuffer] {
    val localBuffer: ByteBuffer = ByteBuffer.allocate(size)

    override def set(start: Int, length: Int, buf: ByteBuffer): Unit = {
      val lim = buf.limit()
      buf.limit(buf.position() + length)
      localBuffer.position(start).limit(start + length)
      localBuffer.put(buf)
      buf.position(buf.limit())
      buf.limit(lim)
    }

    override def copyTo(buffer: LocalRenderBuffer, fromIdx: Int, toIdx: Int, len: Int): Unit = {
      localBuffer.position(fromIdx).limit(fromIdx + len)
      buffer.localBuffer.position(toIdx)
      buffer.localBuffer.put(localBuffer)
    }

    override def render(length: Int): Unit = ???

    override def unload(): Unit = ???
  }
}
