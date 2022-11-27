package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords

import com.flowpowered.nbt.{CompoundTag, Tag}
import org.joml.{Matrix4f, Vector3d}

class EntityBaseData(
    var position: CylCoords,
    var rotation: Vector3d = new Vector3d,
    var velocity: Vector3d = new Vector3d
):
  def toNBT: Seq[Tag[_]] = Seq(
    NBTUtil.makeVectorTag("pos", position.toVector3d),
    NBTUtil.makeVectorTag("velocity", velocity),
    NBTUtil.makeVectorTag("rotation", rotation)
  )

object EntityBaseData:
  def fromNBT(tag: CompoundTag)(using CylinderSize): EntityBaseData =
    val position = NBTUtil
      .getCompoundTag(tag, "pos")
      .map(t => CylCoords(NBTUtil.setVector(t, new Vector3d)))
      .getOrElse(CylCoords(0, 0, 0))

    val data = new EntityBaseData(position = position)
    NBTUtil.getCompoundTag(tag, "velocity").foreach(t => NBTUtil.setVector(t, data.velocity))
    NBTUtil.getCompoundTag(tag, "rotation").foreach(t => NBTUtil.setVector(t, data.rotation))
    data

abstract class Entity(protected val data: EntityBaseData)(using CylinderSize) {
  def model: EntityModel
  def id: String

  def position: CylCoords = data.position
  def position_=(pos: CylCoords): Unit = data.position = pos
  def rotation: Vector3d = data.rotation
  def velocity: Vector3d = data.velocity

  def boundingBox: HexBox = new HexBox(0.5f, 0, 0.5f)

  def transform: Matrix4f = new Matrix4f()
    .translate(data.position.toVector3f)
    .rotateZ(data.rotation.z.toFloat)
    .rotateX(data.rotation.x.toFloat)
    .rotateY(data.rotation.y.toFloat)

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = ()

  def toNBT: Seq[Tag[_]] = data.toNBT
}
