package hexacraft.server.world

import hexacraft.physics.{Density, DragCoefficient, FluidDynamics}
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.{MotionComponent, TransformComponent}

import org.joml.Vector3d

class EntityPhysicsSystem(world: BlocksInWorld, collisionDetector: CollisionDetector)(using
    CylinderSize
) {

  /** @param volume
    *   the volume of the entity (from its model), used for buoyancy and drag
    */
  def update(
      transform: TransformComponent,
      motion: MotionComponent,
      boundingBox: HexBox,
      volume: VolumeSamples,
      coefficient: DragCoefficient
  ): Unit = {
    val volumeInWater = volume.volumeInWater(world, transform.position, transform.rotation)

    applyBuoyancy(motion.velocity, 75, volumeInWater, Density.water)

    val isMoving = motion.velocity.lengthSquared > 0
    if isMoving && volume.totalVolume > 0 then {
      val totalArea = boundingBox.projectedAreaInDirection(motion.velocity)
      val adjustedArea = totalArea * (volumeInWater / volume.totalVolume)
      applyDrag(motion.velocity, coefficient, 75, adjustedArea)
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

  private def applyDrag(
      velocity: Vector3d,
      coefficient: DragCoefficient,
      objectMass: Double,
      objectProjectedArea: Double
  ): Unit = {
    val drag = FluidDynamics.dragForce(velocity, coefficient, objectProjectedArea, Density.water)

    // dv = a * dt = (F / m) * (1 / 60) = F / (m * 60)
    velocity.add(drag.div(objectMass * 60))
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
