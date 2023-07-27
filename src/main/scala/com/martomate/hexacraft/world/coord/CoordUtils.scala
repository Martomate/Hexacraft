package com.martomate.hexacraft.world.coord

import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld, Offset}
import org.joml.Vector3d

import scala.annotation.tailrec

object CoordUtils {
  def getEnclosingBlock(vec: BlockCoords)(using CylinderSize): (BlockRelWorld, BlockCoords.Offset) = {
    val (x, y, z) = (vec.x, vec.y, vec.z)

    @tailrec
    def findBlockPos(xInt: Int, zInt: Int): (Int, Int) = {
      val xx = x - xInt
      val zz = z - zInt

      val xp = xx + 0.5 * zz
      val zp = zz + 0.5 * xx
      val wp = zp - xp

      if (xp > 0.5) findBlockPos(xInt + 1, zInt)
      else if (xp < -0.5) findBlockPos(xInt - 1, zInt)
      else if (zp > 0.5) findBlockPos(xInt, zInt + 1)
      else if (zp < -0.5) findBlockPos(xInt, zInt - 1)
      else if (wp > 0.5) findBlockPos(xInt - 1, zInt + 1)
      else if (wp < -0.5) findBlockPos(xInt + 1, zInt - 1)
      else (xInt, zInt)
    }

    val (xInt, zInt) = findBlockPos(math.round(x).toInt, math.round(z).toInt)

    val xx = x - xInt
    val zz = z - zInt
    val yInt = math.floor(y).toInt

    (BlockRelWorld(xInt, yInt, zInt), BlockCoords.Offset(xx, y - yInt, zz))
  }

  def approximateIntCoords(coords: BlockCoords)(using CylinderSize): BlockRelWorld = {
    val X = math.round(coords.x).toInt
    val Y = math.round(coords.y).toInt
    val Z = math.round(coords.z).toInt
    BlockRelWorld(X, Y, Z)
  }

  def approximateChunkCoords(coords: CylCoords)(using CylinderSize): ChunkRelWorld =
    approximateIntCoords(coords.toBlockCoords).getChunkRelWorld

  def vectorToOffset(vec: Vector3d): Offset = {
    val blockCoords = CylCoords.Offset(vec).toBlockCoordsOffset
    Offset(blockCoords.x.round.toInt, blockCoords.y.round.toInt, blockCoords.z.round.toInt)
  }
}
