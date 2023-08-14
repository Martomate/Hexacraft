package hexacraft.world.entity

import hexacraft.physics.Density
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize}
import hexacraft.world.block.{Blocks, HexBox}
import hexacraft.world.coord.fp.{BlockCoords, CylCoords}

import org.joml.{Vector3d, Vector3f}

class EntityPhysicsSystem(world: BlocksInWorld, collisionDetector: CollisionDetector)(using Blocks: Blocks)(using
    CylinderSize
) {
  def update(data: EntityBaseData, boundingBox: HexBox): Unit =
    applyBuoyancy(data.velocity, 75, volumeSubmergedInWater(boundingBox, data.position), Density.water)

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

  private def volumeSubmergedInWater(bounds: HexBox, position: CylCoords): Double =
    val solidBounds = bounds.scaledRadially(0.7)
    solidBounds
      .cover(position)
      .map(c => c -> world.getBlock(c))
      .filter((c, b) => b.blockType == Blocks.Water)
      .map((c, b) =>
        HexBox.approximateVolumeOfIntersection(
          BlockCoords(c).toCylCoords,
          b.blockType.bounds(b.metadata),
          position,
          solidBounds
        )
      )
      .sum

  private def applyBuoyancy(
      velocity: Vector3d,
      objectMass: Double,
      submergedVolume: Double,
      fluidDensity: Density
  ): Unit =
    velocity.y += (submergedVolume * fluidDensity.toSI * 9.82) / (objectMass * 60)
}

// TODO: Collision detection.
// There needs to be a method taking and entity and the world, returning collision info
// The question is: Who is responsible for checking collision? The entity, the chunk or the world?
