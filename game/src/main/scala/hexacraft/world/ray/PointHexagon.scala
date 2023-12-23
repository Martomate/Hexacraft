package hexacraft.world.ray

import hexacraft.world.{Camera, CylinderSize}
import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.{BlockCoords, CylCoords, NormalCoords}
import hexacraft.world.coord.integer.BlockRelWorld

import org.joml.Vector3d

object PointHexagon:
  def fromHexBox(hexBox: HexBox, location: BlockRelWorld, camera: Camera)(using CylinderSize): PointHexagon =
    val points =
      for v <- hexBox.vertices
      yield asNormalCoords(location, v, camera).toVector3d
    new PointHexagon(points)

  private def asNormalCoords(blockPos: BlockRelWorld, offset: CylCoords.Offset, camera: Camera)(using
      CylinderSize
  ): NormalCoords =
    val blockCoords = BlockCoords(blockPos).toCylCoords.offset(offset)
    blockCoords.toNormalCoords(CylCoords(camera.view.position))

class PointHexagon(points: Seq[Vector3d]):
  def up(idx: Int): Vector3d = points(idx)
  def down(idx: Int): Vector3d = points(idx + 6)
