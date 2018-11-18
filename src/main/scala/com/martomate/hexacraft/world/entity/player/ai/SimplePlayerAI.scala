package com.martomate.hexacraft.world.entity.player.ai

import com.martomate.hexacraft.util.MathUtils
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.player.PlayerEntity
import org.joml.{Vector3d, Vector3dc}

class SimplePlayerAI(player: PlayerEntity) extends PlayerAI {
  private val movingForce = new Vector3d
  private var target: CylCoords = CylCoords(0, 0, 0)(player.position.cylSize)

  private var timeout: Int = 0

  private val timeLimit: Int = 5 * 60
  private val reach: Double = 5
  private val speed = 0.2

  def tick(): Unit = {
    val distSq = player.position.distanceXZSq(target)

    movingForce.set(0)

    if (distSq < speed * speed || timeout == 0) {
      // new goal
      val angle = math.random() * 2 * math.Pi
      val targetX = player.position.x + reach * math.cos(angle)
      val targetZ = player.position.z + reach * -math.sin(angle)
      target = CylCoords(targetX, 0, targetZ)(player.position.cylSize)

      timeout = timeLimit
    } else {
      // move towards goal
      val blockInFront = player.blockInFront(player.boundingBox.radius + speed * 4)

      if (blockInFront != Blocks.Air && player.velocity.y == 0) {
        movingForce.y = 5
      }
      val angle = player.position.angleXZ(target)

      movingForce.x = speed * math.cos(angle)
      movingForce.z = speed * math.sin(angle)
      player.rotation.y = -angle.toFloat
    }

    timeout -= 1

/*    val speed = 40
    val velLen = math.hypot(player.velocity.x, player.velocity.z)
    if (velLen > speed) {
      velocity.x *= speed / velLen
      velocity.z *= speed / velLen
    }*/
  }

  override def acceleration(): Vector3dc = movingForce
}
