package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.util.{CylinderSize, Nbt, NBTUtil}
import com.martomate.hexacraft.world.coord.fp.CylCoords

import com.flowpowered.nbt.{CompoundTag, Tag}
import org.joml.Matrix4f
import org.joml.Vector3d

class EntityBaseData(
    var position: CylCoords,
    var rotation: Vector3d = new Vector3d,
    var velocity: Vector3d = new Vector3d
):
  def transform: Matrix4f = new Matrix4f()
    .translate(position.toVector3f)
    .rotateZ(rotation.z.toFloat)
    .rotateX(rotation.x.toFloat)
    .rotateY(rotation.y.toFloat)

  def toNBT: EntityBaseData.NbtData = EntityBaseData.NbtData(
    pos = Nbt.from(NBTUtil.makeVectorTag("pos", position.toVector3d)),
    velocity = Nbt.from(NBTUtil.makeVectorTag("velocity", velocity)),
    rotation = Nbt.from(NBTUtil.makeVectorTag("rotation", rotation))
  )

object EntityBaseData:
  case class NbtData(pos: Nbt.MapTag, velocity: Nbt.MapTag, rotation: Nbt.MapTag)

  def fromNBT(tag2: CompoundTag)(using CylinderSize): EntityBaseData =
    val tag = Nbt.from(tag2)
    val position = NBTUtil
      .getCompoundTag(tag, "pos")
      .map(t => CylCoords(NBTUtil.setVector(t, new Vector3d)))
      .getOrElse(CylCoords(0, 0, 0))

    val data = new EntityBaseData(position = position)
    NBTUtil.getCompoundTag(tag, "velocity").foreach(t => NBTUtil.setVector(t, data.velocity))
    NBTUtil.getCompoundTag(tag, "rotation").foreach(t => NBTUtil.setVector(t, data.rotation))
    data
