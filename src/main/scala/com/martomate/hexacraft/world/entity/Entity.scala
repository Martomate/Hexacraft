package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.{Matrix4f, Vector3d, Vector3f}

abstract class Entity(init_pos: CylCoords) {
  def model: EntityModel

  private var _position: CylCoords = init_pos
  def position: CylCoords = _position
  protected def position_=(pos: CylCoords): Unit = _position = pos

  private var _rotation: Vector3f = new Vector3f
  def rotation: Vector3f = _rotation
  protected def rotation_=(rot: Vector3f): Unit = _rotation = rot

  private var _velocity: Vector3d = new Vector3d
  def velocity: Vector3d = _velocity
  protected def velocity_=(vel: Vector3d): Unit = _velocity = vel

  def boundingBox: HexBox = new HexBox(0.5f, 0, 0.5f)

  def transform: Matrix4f = new Matrix4f()
    .translate(position.toVector3f)
    .rotateZ(rotation.z)
    .rotateX(rotation.x)
    .rotateY(rotation.y)

  def tick(): Unit = ()
}
