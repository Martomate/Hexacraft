package hexacraft.client.render

import hexacraft.infra.gpu.OpenGL
import hexacraft.renderer.{GpuState, Renderer, VAO, VBO}
import hexacraft.shaders.BlockShader
import hexacraft.util.{Segment, SegmentSet}

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

class BufferHandler[T <: RenderBuffer[T]](
    verticesPerBuffer: Int,
    bytesPerVertex: Int,
    bufferAllocator: RenderBuffer.Allocator[T]
) {
  private val bufSize: Int = verticesPerBuffer * bytesPerVertex

  private val buffers: ArrayBuffer[T] = ArrayBuffer.empty

  def set(seg: Segment, data: ByteBuffer): Unit = {
    require(seg.length <= data.remaining())
    val startBuf = seg.start / bufSize
    val endBuf = (seg.start + seg.length - 1) / bufSize
    var left = seg.length
    var pos = seg.start
    for i <- startBuf to endBuf do {
      val start = pos % bufSize
      val amt = math.min(left, bufSize - start)

      pos += amt
      left -= amt

      if i >= buffers.length then {
        val buf = bufferAllocator.allocate(verticesPerBuffer)
        buffers += buf
      }

      val lim = data.limit()
      data.limit(data.position() + amt)
      buffers(i).set(start, data)
      data.position(data.limit())
      data.limit(lim)
    }
  }

  /** This probably only works if to &lt;= from */
  def copy(from: Int, to: Int, len: Int): Unit = {
    var fromBuffer: Int = from / bufSize
    var fromIdx: Int = from % bufSize
    var toBuffer: Int = to / bufSize
    var toIdx: Int = to % bufSize
    var left: Int = len

    while left > 0 do {
      val leftF = bufSize - fromIdx
      val leftT = bufSize - toIdx
      val len = math.min(math.min(leftF, leftT), left)

      bufferAllocator.copy(buffers(fromBuffer), buffers(toBuffer), fromIdx, toIdx, len)

      fromIdx = (fromIdx + len) % bufSize
      toIdx = (toIdx + len) % bufSize

      if leftF == len then {
        fromBuffer += 1
      }
      if leftT == len then {
        toBuffer += 1
      }

      left -= len
    }
  }

  def render(length: Int): Unit = {
    if length > 0 then {
      for i <- 0 to (length - 1) / bufSize do {
        val len = math.min(length - i * bufSize, bufSize)
        buffers(i).render(len)
      }
    }
  }

  def render(segments: SegmentSet): Unit = {
    for s <- segments do {
      if s.length > 0 then {
        val lo = s.start
        val hi = s.start + s.length
        for i <- lo / bufSize to (hi - 1) / bufSize do {
          val start = math.max(lo - i * bufSize, 0)
          val end = math.min(hi - i * bufSize, bufSize)
          val len = end - start
          buffers(i).render(start, len)
        }
      }
    }
  }

  def unload(): Unit = {
    for b <- buffers do {
      b.unload()
    }
  }
}

object RenderBuffer {
  trait Allocator[T <: RenderBuffer[T]] {
    def allocate(numVertices: Int): T

    def copy(from: T, to: T, fromIdx: Int, toIdx: Int, len: Int): Unit
  }
}

trait RenderBuffer[B <: RenderBuffer[B]] {
  def set(start: Int, buf: ByteBuffer): Unit

  def render(length: Int): Unit
  def render(offset: Int, length: Int): Unit

  def unload(): Unit
}

object VaoRenderBuffer {
  class Allocator(side: Int, gpuState: GpuState) extends RenderBuffer.Allocator[VaoRenderBuffer] {
    override def allocate(numVertices: Int): VaoRenderBuffer = {
      val vao = BlockShader.createVao(side, numVertices)
      new VaoRenderBuffer(
        vao,
        vao.vbos(0),
        new Renderer(OpenGL.PrimitiveMode.Triangles, gpuState)
      )
    }

    override def copy(from: VaoRenderBuffer, to: VaoRenderBuffer, fromIdx: Int, toIdx: Int, len: Int): Unit = {
      VBO.copy(from.vboToFill, to.vboToFill, fromIdx, toIdx, len)
    }
  }
}

class VaoRenderBuffer(vao: VAO, val vboToFill: VBO, renderer: Renderer) extends RenderBuffer[VaoRenderBuffer] {
  override def set(start: Int, buf: ByteBuffer): Unit = {
    val vbo = vboToFill
    vbo.fill(start / vbo.stride, buf)
  }

  def render(length: Int): Unit = {
    renderer.render(vao, length / vboToFill.stride)
  }

  def render(offset: Int, length: Int): Unit = {
    val stride = vboToFill.stride
    renderer.render(vao, offset / stride, length / stride)
  }

  override def unload(): Unit = {
    vao.free()
  }
}
