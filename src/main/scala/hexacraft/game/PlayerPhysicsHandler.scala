package hexacraft.game

import hexacraft.physics.{Density, DragCoefficient, FluidDynamics, Viscosity}
import hexacraft.world.{BlocksInWorld, CollisionDetector}
import hexacraft.world.block.HexBox
import hexacraft.world.player.Player

import org.joml.Vector3d

class PlayerPhysicsHandler(player: Player, world: BlocksInWorld, collisionDetector: CollisionDetector):
  def tick(maxSpeed: Double, effectiveViscosity: Double, volumeSubmergedInWater: Double): Unit =
    val velLen = math.hypot(player.velocity.x, player.velocity.z)
    if velLen > maxSpeed
    then
      player.velocity.x *= maxSpeed / velLen
      player.velocity.z *= maxSpeed / velLen

    if player.flying
    then
      player.position.add(player.velocity.x / 60, player.velocity.y / 60, player.velocity.z / 60)
      player.velocity.x *= 0.8
      player.velocity.z *= 0.8
    else
      if effectiveViscosity < Viscosity.air.toSI * 2 then
        player.velocity.x *= 0.8
        player.velocity.z *= 0.8
      else
        player.velocity.x *= 0.95
        player.velocity.z *= 0.95

      if player.velocity.lengthSquared > 0 then
        val area = player.bounds.projectedAreaInDirection(player.velocity)
        applyDrag(player.velocity, 75, area * (volumeSubmergedInWater / player.bounds.volume))
      applyBuoyancy(player.velocity, 75, volumeSubmergedInWater, Density.water)
      applyGravity(player.velocity)
      applyCollision(player.position, player.velocity, player.bounds)

  private def applyDrag(velocity: Vector3d, objectMass: Double, objectProjectedArea: Double): Unit =
    val drag = FluidDynamics.dragForce(velocity, DragCoefficient.human, objectProjectedArea, Density.water)

    // dv = a * dt = (F / m) * (1 / 60) = F / (m * 60)
    velocity.add(drag.div(objectMass * 60))

  private def applyBuoyancy(
      velocity: Vector3d,
      objectMass: Double,
      submergedVolume: Double,
      fluidDensity: Density
  ): Unit =
    velocity.y += (submergedVolume * fluidDensity.toSI * 9.82) / (objectMass * 60)

  private def applyGravity(velocity: Vector3d): Unit =
    velocity.y -= 9.82 / 60

  private def applyCollision(position: Vector3d, velocity: Vector3d, bounds: HexBox): Unit =
    velocity.div(60)
    val (pos, vel) = collisionDetector.positionAndVelocityAfterCollision(bounds, position, velocity)
    position.set(pos)
    velocity.set(vel)
    velocity.mul(60)
