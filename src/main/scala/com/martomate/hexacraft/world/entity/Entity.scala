package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.{BlocksInWorld, CollisionDetector}
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords

import com.flowpowered.nbt.Tag
import org.joml.{Matrix4f, Vector3d}

class Entity(protected val data: EntityBaseData, val model: EntityModel) {
  def position: CylCoords = data.position
  def rotation: Vector3d = data.rotation
  def velocity: Vector3d = data.velocity

  def boundingBox: HexBox = new HexBox(0.5f, 0, 0.5f)

  def transform: Matrix4f = data.transform

  def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = ()

  def toNBT: Seq[Tag[_]] = data.toNBT
}
