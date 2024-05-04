package hexacraft.game

import hexacraft.physics.{Density, DragCoefficient, FluidDynamics, Viscosity}
import hexacraft.world.{CollisionDetector, HexBox, Player}

import org.joml.Vector3d

class PlayerPhysicsHandler(collisionDetector: CollisionDetector) {
  def tick(player: Player, maxSpeed: Double, effectiveViscosity: Double, volumeSubmergedInWater: Double): Unit = {
    val velLen = math.hypot(player.velocity.x, player.velocity.z)
    if velLen > maxSpeed then {
      player.velocity.x *= maxSpeed / velLen
      player.velocity.z *= maxSpeed / velLen
    }

    if player.flying then {
      player.position.add(player.velocity.x / 60, player.velocity.y / 60, player.velocity.z / 60)
      player.velocity.x *= 0.8
      player.velocity.z *= 0.8
      return
    }

    val frictionFactor = if effectiveViscosity < Viscosity.air.toSI * 2 then 0.8 else 0.95
    player.velocity.x *= frictionFactor
    player.velocity.z *= frictionFactor

    val isMoving = player.velocity.lengthSquared > 0
    if isMoving then {
      val totalArea = player.bounds.projectedAreaInDirection(player.velocity)
      val adjustedArea = totalArea * (volumeSubmergedInWater / player.bounds.volume)
      applyDrag(player.velocity, 75, adjustedArea)
    }

    applyBuoyancy(player.velocity, 75, volumeSubmergedInWater, Density.water)
    applyGravity(player.velocity)
    applyCollision(player.position, player.velocity, player.bounds)
  }

  private def applyDrag(velocity: Vector3d, objectMass: Double, objectProjectedArea: Double): Unit = {
    val drag = FluidDynamics.dragForce(velocity, DragCoefficient.human, objectProjectedArea, Density.water)

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

  private def applyGravity(velocity: Vector3d): Unit = {
    velocity.y -= 9.82 / 60
  }

  private def applyCollision(position: Vector3d, velocity: Vector3d, bounds: HexBox): Unit = {
    velocity.div(60)
    val (pos, vel) = collisionDetector.positionAndVelocityAfterCollision(bounds, position, velocity)
    position.set(pos)
    velocity.set(vel)
    velocity.mul(60)
  }
}
