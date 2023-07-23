package com.martomate.hexacraft.world.ray

import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords, NormalCoords}
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

import org.joml.Vector3d

object PointHexagon:
  def fromHexBox(hexBox: HexBox, location: BlockRelWorld, camera: Camera)(implicit
      cylSize: CylinderSize
  ): PointHexagon =
    val points =
      for v <- hexBox.vertices
      yield asNormalCoords(location, v, camera).toVector3d
    new PointHexagon(points)

  private def asNormalCoords(blockPos: BlockRelWorld, offset: CylCoords.Offset, camera: Camera)(implicit
      cylSize: CylinderSize
  ): NormalCoords =
    val blockCoords = BlockCoords(blockPos).toCylCoords.offset(offset)
    blockCoords.toNormalCoords(CylCoords(camera.view.position))

class PointHexagon(points: Seq[Vector3d]):
  def up(idx: Int): Vector3d = points(idx)
  def down(idx: Int): Vector3d = points(idx + 6)
