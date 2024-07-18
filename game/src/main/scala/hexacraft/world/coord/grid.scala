package hexacraft.world.coord

import hexacraft.math.bits.{Int12, Int20}
import hexacraft.world.CylinderSize

import org.joml.Vector2d

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

case class Offset(dx: Int, dy: Int, dz: Int) {
  def +(other: Offset): Offset = {
    Offset(
      dx + other.dx,
      dy + other.dy,
      dz + other.dz
    )
  }

  def -(other: Offset): Offset = {
    Offset(
      dx - other.dx,
      dy - other.dy,
      dz - other.dz
    )
  }

  def manhattanDistance: Int = {
    math.abs(dy) + math.max(math.max(math.abs(dx), math.abs(dz)), math.abs(dx + dz))
  }
}

object NeighborOffsets {
  val all: Array[Offset] = Array(
    Offset(0, 1, 0),
    Offset(0, -1, 0),
    Offset(1, 0, 0),
    Offset(0, 0, 1),
    Offset(-1, 0, 1),
    Offset(-1, 0, 0),
    Offset(0, 0, -1),
    Offset(1, 0, -1)
  )

  /** @param side
    * the side of the origin block (0: top, 1: bottom, 2-7: sides)
    * @return
    * The offset of the neighboring block on the given side
    */
  inline def apply(side: Int): Offset = all(side)

  inline def indices: Range = all.indices
}

object BlockRelChunk {
  def apply(x: Int, y: Int, z: Int): BlockRelChunk = BlockRelChunk((x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf))
}

case class BlockRelChunk(value: Int) extends AnyVal { // xyz
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte

  def offset(off: Offset): BlockRelChunk = offset(off.dx, off.dy, off.dz)
  def offset(x: Int, y: Int, z: Int): BlockRelChunk = BlockRelChunk(cx + x, cy + y, cz + z)

  def isOnChunkEdge(side: Int): Boolean = {
    val off = NeighborOffsets(side)
    val xx = cx + off.dx
    val yy = cy + off.dy
    val zz = cz + off.dz
    (xx & ~15 | yy & ~15 | zz & ~15) != 0
  }

  def neighbor(side: Int): BlockRelChunk = {
    val off = NeighborOffsets(side)
    val xx = cx + off.dx
    val yy = cy + off.dy
    val zz = cz + off.dz
    BlockRelChunk(xx, yy, zz)
  }

  def globalNeighbor(side: Int, chunk: ChunkRelWorld)(using CylinderSize): BlockRelWorld = {
    val off = NeighborOffsets(side)
    val xx = cx + off.dx
    val yy = cy + off.dy
    val zz = cz + off.dz
    BlockRelWorld(xx, yy, zz, chunk)
  }

  override def toString: String = s"($cx, $cy, $cz)"
}

object BlockRelWorld {
  def apply(X: Long, Y: Int, Z: Long, x: Int, y: Int, z: Int)(using cylSize: CylinderSize): BlockRelWorld = {
    val chunk = (X & 0xfffff) << 32L | (Z & cylSize.ringSizeMask) << 12L | (Y & 0xfff)
    val block = (x & 0xf) << 8 | (y & 0xf) << 4 | (z & 0xf)

    BlockRelWorld(chunk << 12 | block)
  }

  def apply(x: Int, y: Int, z: Int)(using CylinderSize): BlockRelWorld = {
    BlockRelWorld(x >> 4, y >> 4, z >> 4, x & 15, y & 15, z & 15)
  }

  def fromChunk(block: BlockRelChunk, chunk: ChunkRelWorld): BlockRelWorld = {
    BlockRelWorld(chunk.value << 12 | block.value)
  }

  def apply(i: Int, j: Int, k: Int, chunk: ChunkRelWorld)(using CylinderSize): BlockRelWorld = {
    BlockRelWorld(chunk.X.toInt * 16 + i, chunk.Y.toInt * 16 + j, chunk.Z.toInt * 16 + k)
  }
}

case class BlockRelWorld(value: Long) extends AnyVal { // XXXXXZZZZZYYYxyz
  def getBlockRelChunk: BlockRelChunk = BlockRelChunk((value & 0xfff).toInt)
  def getChunkRelWorld: ChunkRelWorld = ChunkRelWorld(value >>> 12)
  def getColumnRelWorld: ColumnRelWorld = ColumnRelWorld(value >>> 24)

  def offset(t: Offset)(using CylinderSize): BlockRelWorld = offset(t.dx, t.dy, t.dz)
  def offset(xx: Int, yy: Int, zz: Int)(using CylinderSize): BlockRelWorld = BlockRelWorld(x + xx, y + yy, z + zz)

  def X: Int20 = Int20.truncate(value >> 44)
  def Z: Int20 = Int20.truncate(value >> 24)
  def Y: Int12 = Int12.truncate(value >>> 12)
  def cx: Byte = (value >> 8 & 0xf).toByte
  def cy: Byte = (value >> 4 & 0xf).toByte
  def cz: Byte = (value >> 0 & 0xf).toByte
  def x: Int = X.toInt << 4 | cx
  def y: Int = Y.toInt << 4 | cy
  def z: Int = Z.toInt << 4 | cz

  override def toString = s"($x, $y, $z)"
}

object ChunkRelWorld {
  def apply(X: Long, Y: Int, Z: Int)(using cylSize: CylinderSize): ChunkRelWorld =
    ChunkRelWorld((X & 0xfffff) << 32 | (Z & cylSize.ringSizeMask) << 12 | (Y & 0xfff))

  def apply(chunk: Int12, column: ColumnRelWorld): ChunkRelWorld =
    ChunkRelWorld(column.value << 12L | chunk.repr.toInt)

  val neighborOffsets: Seq[Offset] = Seq(
    Offset(0, 1, 0),
    Offset(1, 0, 0),
    Offset(0, 0, 1),
    Offset(-1, 0, 1),
    Offset(0, -1, 0),
    Offset(-1, 0, 0),
    Offset(0, 0, -1),
    Offset(1, 0, -1)
  )
}

case class ChunkRelWorld(value: Long) extends AnyVal { // XXXXXZZZZZYYY
  def getColumnRelWorld: ColumnRelWorld = ColumnRelWorld(value >>> 12)

  def X: Int20 = Int20.truncate(value >> 32)
  def Z: Int20 = Int20.truncate(value >> 12)
  def Y: Int12 = Int12.truncate(value)

  def neighbors(using CylinderSize): Seq[ChunkRelWorld] =
    ChunkRelWorld.neighborOffsets.map(offset)

  def extendedNeighbors(radius: Int)(using CylinderSize): Seq[ChunkRelWorld] = {
    val s = 2 * radius + 1
    val buf = new mutable.ArrayBuffer[ChunkRelWorld](s * s * s)

    for {
      y <- -radius to radius
      z <- -radius to radius
      x <- -radius to radius
    } do {
      buf += offset(x, y, z)
    }

    buf.toSeq
  }

  def offset(t: Offset)(using CylinderSize): ChunkRelWorld = offset(t.dx, t.dy, t.dz)
  def offset(dx: Int, dy: Int, dz: Int)(using CylinderSize): ChunkRelWorld = ChunkRelWorld(
    X.toInt + dx,
    Y.toInt + dy,
    Z.toInt + dz
  )

  override def toString: String = s"(${X.toInt}, ${Y.toInt}, ${Z.toInt})"
}

object ColumnRelWorld {
  def apply(X: Long, Z: Int)(using cylSize: CylinderSize): ColumnRelWorld = {
    ColumnRelWorld((X & 0xfffff) << 20 | (Z & cylSize.ringSizeMask))
  }
}

case class ColumnRelWorld(value: Long) extends AnyVal { // XXXXXZZZZZ
  def X: Int20 = Int20.truncate(value >> 20)
  def Z: Int20 = Int20.truncate(value)

  def offset(x: Int, z: Int)(using CylinderSize): ColumnRelWorld = ColumnRelWorld(X.toInt + x, Z.toInt + z)

  def distSq(origin: Vector2d)(using cylSize: CylinderSize): Double = {
    val dx = this.X.toInt - origin.x + 0.5
    val dz1 = math.abs(this.Z.toInt - origin.y + 0.5 + dx * 0.5f)
    val dz2 = math.abs(dz1 - cylSize.ringSize)
    val dz = math.min(dz1, dz2)
    dx * dx + dz * dz
  }
}
