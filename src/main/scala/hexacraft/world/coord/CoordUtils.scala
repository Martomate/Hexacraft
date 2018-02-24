package hexacraft.world.coord

import hexacraft.Camera
import hexacraft.world.storage.World
import org.joml.Vector3d

object CoordUtils {
  val y60: Double = Math.sqrt(3) / 2

  def toBlockCoords(camera: Camera, world: World, vec: Vector3d, adjustY: Boolean = true): (BlockRelWorld, BlockCoords) = {
    val camX = camera.position.x
    val y = vec.y * 2
    val mult = if (adjustY) Math.exp(y - camera.position.y) else 1
    val x = ((vec.x - camX) / mult + camX) / 0.75
    val z = vec.z / y60 - x / 2

    toBlockCoords(BlockCoords(x, y, z, camera.world))
  }
  
  def toBlockCoords(vec: BlockCoords): (BlockRelWorld, BlockCoords) = {
    val (x, y, z) = (vec.x, vec.y, vec.z)
    
    def findBlockPos(xInt: Int, zInt: Int): (Int, Int) = {
      val xx = x - xInt
      val zz = z - zInt
      
      val xp = xx + 0.5 * zz
      val zp = zz + 0.5 * xx
      val wp = zp - xp

      if      (xp >  0.5) findBlockPos(xInt + 1, zInt    )
      else if (xp < -0.5) findBlockPos(xInt - 1, zInt    )
      else if (zp >  0.5) findBlockPos(xInt    , zInt + 1)
      else if (zp < -0.5) findBlockPos(xInt    , zInt - 1)
      else if (wp >  0.5) findBlockPos(xInt - 1, zInt + 1)
      else if (wp < -0.5) findBlockPos(xInt + 1, zInt - 1)
      else                (xInt, zInt)
    }

    val (xInt, zInt) = findBlockPos(math.round(x).toInt, math.round(z).toInt)

    val xx = x - xInt
    val zz = z - zInt
    val yInt = math.floor(y).toInt

    (BlockRelWorld(xInt, yInt, zInt, vec.world), new BlockCoords(xx, y - yInt, zz, vec.world, false))
  }

  def fromBlockCoords(world: World, camera: Camera, blockPos: BlockRelWorld, position: CylCoords, _result: Vector3d): Vector3d =
    fromBlockCoords(world, camera, BlockCoords(blockPos.x, blockPos.y, blockPos.z, world), position, _result)

  def fromBlockCoords(world: World, camera: Camera, blockPos: BlockCoords, position: CylCoords, _result: Vector3d): Vector3d = {
    val pos = if (_result != null) _result else new Vector3d()
    (blockPos.toCylCoord + position).toNormalCoord(CylCoords(camera.position, world)) into pos
  }

  def fitZ(z: Double, circumference: Double): Double = {
    val zz = z % circumference
    if (zz < 0) {
      zz + circumference
    } else {
      zz
    }
  }
}
