package com.martomate.hexacraft.game

import com.martomate.hexacraft.world.CollisionDetector
import com.martomate.hexacraft.world.player.Player

class PlayerPhysicsHandler(player: Player, collisionDetector: CollisionDetector):
  def tick(maxSpeed: Double): Unit =
    player.velocity.x *= 0.75
    player.velocity.z *= 0.75

    val velLen = math.hypot(player.velocity.x, player.velocity.z)
    if velLen > maxSpeed
    then
      player.velocity.x *= maxSpeed / velLen
      player.velocity.z *= maxSpeed / velLen

    if player.flying
    then player.position.add(player.velocity.x / 60, player.velocity.y / 60, player.velocity.z / 60)
    else
      player.velocity.y -= 9.82 / 60
      player.velocity.div(60)
      val (pos, vel) = collisionDetector.positionAndVelocityAfterCollision(
        player.bounds,
        player.position,
        player.velocity
      )
      player.position.set(pos)
      player.velocity.set(vel)
      player.velocity.mul(60)
