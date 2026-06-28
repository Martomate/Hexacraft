package hexacraft.world.entity

import hexacraft.physics.{Density, DragCoefficient, FluidDynamics}
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize, HexBox}
import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockCoords, CylCoords}

import org.joml.Vector3d

class EntityPhysicsSystem(world: BlocksInWorld, collisionDetector: CollisionDetector)(using
    CylinderSize
) {
  def update(transform: TransformComponent, motion: MotionComponent, boundingBox: HexBox): Unit = {
    applyBuoyancy(motion.velocity, 75, volumeSubmergedInWater(boundingBox, transform.position), Density.water)

    val isMoving = motion.velocity.lengthSquared > 0
    if isMoving then {
      val totalArea = boundingBox.projectedAreaInDirection(motion.velocity)
      val adjustedArea = totalArea * (volumeSubmergedInWater(boundingBox, transform.position) / boundingBox.volume)
      applyDrag(motion.velocity, 75, adjustedArea)
    }

    if !motion.flying then {
      motion.velocity.y -= 9.82 / 60
    }
    motion.velocity.div(60)

    val (pos, vel) = collisionDetector.positionAndVelocityAfterCollision(
      boundingBox,
      transform.position.toVector3d,
      motion.velocity
    )
    transform.position = CylCoords(pos)
    motion.velocity.set(vel)

    motion.velocity.mul(60)
  }

  private def applyDrag(velocity: Vector3d, objectMass: Double, objectProjectedArea: Double): Unit = {
    val drag = FluidDynamics.dragForce(velocity, DragCoefficient.human, objectProjectedArea, Density.water)

    // dv = a * dt = (F / m) * (1 / 60) = F / (m * 60)
    velocity.add(drag.div(objectMass * 60))
  }

  private def volumeSubmergedInWater(bounds: HexBox, position: CylCoords): Double = {
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
  }

  private def applyBuoyancy(
      velocity: Vector3d,
      objectMass: Double,
      submergedVolume: Double,
      fluidDensity: Density
  ): Unit = {
    velocity.y += (submergedVolume * fluidDensity.toSI * 9.82) / (objectMass * 60)
  }
}
