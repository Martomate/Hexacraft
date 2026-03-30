package hexacraft.client.render

import hexacraft.shaders.TerrainShader
import hexacraft.util.{KeyedSegment, Loop}
import hexacraft.world.coord.ChunkRelWorld

import org.joml.Vector3f

import java.nio.ByteBuffer
import scala.collection.mutable

class TerrainBatchRenderer(bufferHandlerFactory: => BufferHandler[?]) {
  private val memorySegmentsPerColor: mutable.Map[Vector3f, KeyedSegment[ChunkRelWorld]] = mutable.Map.empty
  private val bufferHandlerPerColor: mutable.Map[Vector3f, BufferHandler[?]] = mutable.Map.empty

  def fragmentation: Float = {
    val total = memorySegmentsPerColor.map(s => s._2.numKeyedSegments.toFloat).sum
    val count = memorySegmentsPerColor.map(s => s._2.numKeys).sum
    total / count
  }
  
  def totalBytes = memorySegmentsPerColor.values.map(_.usedSegments.totalLength).sum

  def isEmpty: Boolean = memorySegmentsPerColor.forall(_._2.isEmpty)

  def render(shader: TerrainShader): Unit = {
    for (color, memorySegments) <- memorySegmentsPerColor do {
      shader.setColor(color)
      bufferHandlerPerColor(color).render(memorySegments.usedSegments)
    }
  }

  def update(
      chunksToClear: IndexedSeq[ChunkRelWorld],
      chunksToUpdate: IndexedSeq[(ChunkRelWorld, Seq[(Vector3f, ByteBuffer)])]
  ): Unit = {
    // Step 1: mark old data as unused
    Loop.array(chunksToClear) { coords =>
      memorySegmentsPerColor.foreach(_._2.clear(coords))
    }
    Loop.array(chunksToUpdate) { case (coords, _) =>
      memorySegmentsPerColor.foreach(_._2.clear(coords))
    }

    // Step 2: fill data into the space of the old data and push the rest at the end
    Loop.array(chunksToUpdate) { case (coords, buffers) =>
      buffers.foreach { case (color, data) =>
        val memorySegments = memorySegmentsPerColor.getOrElseUpdate(color, new KeyedSegment[ChunkRelWorld])
        val segments = memorySegments.allocate(coords, data.remaining())
        Loop.iterate(segments.iterator) { s =>
          bufferHandlerPerColor.getOrElseUpdate(color, bufferHandlerFactory).set(s, data)
        }
      }
    }
  }

  def unload(): Unit = {
    bufferHandlerPerColor.foreach(_._2.unload())
  }
}
