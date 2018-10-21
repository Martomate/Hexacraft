package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.collision.CollisionDetector
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.worldlike.IWorld

class TempEntity(initPos: CylCoords, override val model: EntityModel, world: IWorld) extends Entity(initPos) {
  import initPos.cylSize.impl

  override def tick(): Unit = {
    velocity.y -= 9.82 / 60
    velocity.div(60)
    val (pos, vel) = CollisionDetector.positionAndVelocityAfterCollision(boundingBox, position.toVector3d, velocity, world)
    position = CylCoords(pos)
    velocity.set(vel)
    velocity.mul(60)

    model.tick()
  }
}
// TODO: Collision detection.
// There needs to be a method taking and entity and the world, returning collision info
// The question is: Who is responsible for checking collision? The entity, the chunk or the world?