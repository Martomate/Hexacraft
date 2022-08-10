package com.martomate.hexacraft.world.entity

import com.flowpowered.nbt.{CompoundTag, Tag}
import com.martomate.hexacraft.util.{CylinderSize, NBTSavable, NBTUtil}
import com.martomate.hexacraft.world.CollisionDetector
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.{Matrix4f, Vector3d}

abstract class Entity(implicit cylSizeImpl: CylinderSize) extends NBTSavable {
  def model: EntityModel
  def id: String

  private var _position: CylCoords = CylCoords(0, 0, 0)
  def position: CylCoords = _position
  def position_=(pos: CylCoords): Unit = _position = pos

  private var _rotation: Vector3d = new Vector3d
  def rotation: Vector3d = _rotation
  protected def rotation_=(rot: Vector3d): Unit = _rotation = rot

  private var _velocity: Vector3d = new Vector3d
  def velocity: Vector3d = _velocity
  protected def velocity_=(vel: Vector3d): Unit = _velocity = vel

  def boundingBox: HexBox = new HexBox(0.5f, 0, 0.5f)

  def transform: Matrix4f = new Matrix4f()
    .translate(position.toVector3f)
    .rotateZ(rotation.z.toFloat)
    .rotateX(rotation.x.toFloat)
    .rotateY(rotation.y.toFloat)

  def tick(collisionDetector: CollisionDetector): Unit = ()

  def fromNBT(tag: CompoundTag): Unit = {
    NBTUtil
      .getCompoundTag(tag, "pos")
      .foreach(t => position = CylCoords(NBTUtil.setVector(t, new Vector3d)))
    NBTUtil.getCompoundTag(tag, "velocity").foreach(t => NBTUtil.setVector(t, velocity))
    NBTUtil.getCompoundTag(tag, "rotation").foreach(t => NBTUtil.setVector(t, rotation))
  }

  def toNBT: Seq[Tag[_]] = Seq(
    NBTUtil.makeVectorTag("pos", position.toVector3d),
    NBTUtil.makeVectorTag("velocity", velocity),
    NBTUtil.makeVectorTag("rotation", rotation)
  )
}
