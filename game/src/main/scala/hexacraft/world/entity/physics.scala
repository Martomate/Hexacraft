package hexacraft.world.entity

import hexacraft.physics.Density
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize, HexBox}
import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockCoords, CylCoords}

import org.joml.Vector3d

class EntityPhysicsSystem(world: BlocksInWorld, collisionDetector: CollisionDetector)(using
    CylinderSize
) {
  def update(transform: TransformComponent, velocity: VelocityComponent, boundingBox: HexBox): Unit =
    applyBuoyancy(velocity.velocity, 75, volumeSubmergedInWater(boundingBox, transform.position), Density.water)

    velocity.velocity.y -= 9.82 / 60
    velocity.velocity.div(60)
    val (pos, vel) = collisionDetector.positionAndVelocityAfterCollision(
      boundingBox,
      transform.position.toVector3d,
      velocity.velocity
    )
    transform.position = CylCoords(pos)
    velocity.velocity.set(vel)
    velocity.velocity.mul(60)

  private def volumeSubmergedInWater(bounds: HexBox, position: CylCoords): Double =
    val solidBounds = bounds.scaledRadially(0.7)
    solidBounds
      .cover(position)
      .map(c => c -> world.getBlock(c))
      .filter((c, b) => b.blockType == Block.Water)
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
