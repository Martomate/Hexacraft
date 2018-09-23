package com.martomate.hexacraft.world.coord

import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import org.joml.Vector3d

object CoordUtils {
  def toBlockCoords(vec: BlockCoords): (BlockRelWorld, BlockCoords) = {
    import vec.cylSize.impl

    val (x, y, z) = (vec.x, vec.y, vec.z)

    def findBlockPos(xInt: Int, zInt: Int): (Int, Int) = {
      val xx = x - xInt
      val zz = z - zInt

      val xp = xx + 0.5 * zz
      val zp = zz + 0.5 * xx
      val wp = zp - xp

      if      (xp >  0.5) findBlockPos(xInt + 1, zInt)
      else if (xp < -0.5) findBlockPos(xInt - 1, zInt)
      else if (zp >  0.5) findBlockPos(xInt,     zInt + 1)
      else if (zp < -0.5) findBlockPos(xInt,     zInt - 1)
      else if (wp >  0.5) findBlockPos(xInt - 1, zInt + 1)
      else if (wp < -0.5) findBlockPos(xInt + 1, zInt - 1)
      else (xInt, zInt)
    }

    val (xInt, zInt) = findBlockPos(math.round(x).toInt, math.round(z).toInt)

    val xx = x - xInt
    val zz = z - zInt
    val yInt = math.floor(y).toInt

    (BlockRelWorld(xInt, yInt, zInt), new BlockCoords(xx, y - yInt, zz, false))
  }

  def fromBlockCoords(reference: CylCoords, blockPos: BlockCoords, position: CylCoords, _result: Vector3d): Vector3d = {
    val pos = if (_result != null) _result else new Vector3d()
    (blockPos.toCylCoords + position).toNormalCoords(reference) into pos
  }
}
