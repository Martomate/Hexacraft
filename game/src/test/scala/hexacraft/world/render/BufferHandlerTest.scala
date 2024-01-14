package hexacraft.world.render

import hexacraft.util.Segment
import hexacraft.world.render.{BufferHandler, RenderBuffer}
import munit.FunSuite

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

class BufferHandlerTest extends FunSuite {
  private val BytesPerInstance: Int = 3
  private val InstancesPerBuffer: Int = 4
  private val BufferSize: Int = BytesPerInstance * InstancesPerBuffer

  test("set should request a new buffer with correct size if needed") {
    val allocator = new LocalRenderBuffer.Allocator
    val handler = new BufferHandler(4, BytesPerInstance, allocator)

    handler.set(Segment(0, 3), rampBuffer(0, 3))

    assertEquals(allocator.instancesRequested.toSeq, Seq(InstancesPerBuffer))
  }

  test("set should complain if the data has smaller length than the segment") {
    val allocator = new LocalRenderBuffer.Allocator
    val handler = new BufferHandler[LocalRenderBuffer](4, BytesPerInstance, allocator)

    intercept[IllegalArgumentException](handler.set(Segment(1, 3), simpleBuffer(3, 2)))
    intercept[IllegalArgumentException](handler.set(Segment(1, 300), simpleBuffer(3, 293)))
  }

  test("set should transfer the data into the correct buffer assuming no border is crossed") {
    val dest = new LocalRenderBuffer(BufferSize)
    val allocator = new LocalRenderBuffer.Allocator(_ => dest)
    val handler = new BufferHandler(4, BytesPerInstance, allocator)

    handler.set(Segment(1, 3), rampBuffer(1, 13))

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

  test("unload should unload all buffers") {
    val dest = new LocalRenderBuffer(BufferSize)
    val allocator = new LocalRenderBuffer.Allocator(_ => dest)
    val handler = new BufferHandler(4, BytesPerInstance, allocator)
    handler.set(Segment(0, 3), rampBuffer(0, 3))

    handler.unload()

    assertEquals(dest.unloadCount, 1)
  }

  def simpleBuffer(start: Int, len: Int): ByteBuffer =
    val bf = ByteBuffer.allocate(start + len + 2)
    bf.position(start).limit(start + len)
    bf

  def filledBuffer(start: Int, len: Int)(filler: Int => Byte): ByteBuffer =
    val buf = simpleBuffer(start, len)
    for (i <- start until start + len) do buf.put(filler(i))
    buf.position(start)
    buf

  def rampBuffer(start: Int, len: Int): ByteBuffer = filledBuffer(start, len)(_.toByte)

  private object LocalRenderBuffer {
    class Allocator(
        mkBuffer: Int => LocalRenderBuffer = _ => new LocalRenderBuffer(BufferSize)
    ) extends RenderBuffer.Allocator[LocalRenderBuffer] {
      val instancesRequested: ArrayBuffer[Int] = ArrayBuffer.empty[Int]

      override def allocate(instances: Int): LocalRenderBuffer =
        instancesRequested += instances
        mkBuffer(instances)

      override def copy(from: LocalRenderBuffer, to: LocalRenderBuffer, fromIdx: Int, toIdx: Int, len: Int): Unit =
        from.localBuffer.position(fromIdx).limit(fromIdx + len)
        to.localBuffer.position(toIdx)
        to.localBuffer.put(from.localBuffer)
    }
  }

  private class LocalRenderBuffer(size: Int) extends RenderBuffer[LocalRenderBuffer] {
    val localBuffer: ByteBuffer = ByteBuffer.allocate(size)
    var unloadCount = 0

    override def set(start: Int, buf: ByteBuffer): Unit = localBuffer.position(start).put(buf)
    override def render(length: Int): Unit = ???
    override def unload(): Unit = unloadCount += 1
  }
}
