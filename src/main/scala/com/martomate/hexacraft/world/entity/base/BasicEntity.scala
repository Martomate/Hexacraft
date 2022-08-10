package com.martomate.hexacraft.world.entity.base

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.CollisionDetector
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{Entity, EntityModel}

abstract class BasicEntity(override val model: EntityModel)(implicit cylSizeImpl: CylinderSize)
    extends Entity {
  override def tick(collisionDetector: CollisionDetector): Unit = {
    velocity.y -= 9.82 / 60
    velocity.div(60)
    val (pos, vel) = collisionDetector.positionAndVelocityAfterCollision(
      boundingBox,
      position.toVector3d,
      velocity
    )
    position = CylCoords(pos)
    velocity.set(vel)
    velocity.mul(60)

    model.tick()
  }
}
// TODO: Collision detection.
// There needs to be a method taking and entity and the world, returning collision info
// The question is: Who is responsible for checking collision? The entity, the chunk or the world?
