package hexacraft.world.entity

import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize}
import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.CylCoords

class EntityPhysicsSystem(world: BlocksInWorld, collisionDetector: CollisionDetector)(using CylinderSize):
  def update(data: EntityBaseData, boundingBox: HexBox): Unit =
    data.velocity.y -= 9.82 / 60
    data.velocity.div(60)
    val (pos, vel) = collisionDetector.positionAndVelocityAfterCollision(
      boundingBox,
      data.position.toVector3d,
      data.velocity
    )
    data.position = CylCoords(pos)
    data.velocity.set(vel)
    data.velocity.mul(60)

end EntityPhysicsSystem

// TODO: Collision detection.
// There needs to be a method taking and entity and the world, returning collision info
// The question is: Who is responsible for checking collision? The entity, the chunk or the world?
