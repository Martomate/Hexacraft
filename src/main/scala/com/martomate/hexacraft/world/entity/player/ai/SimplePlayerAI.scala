package com.martomate.hexacraft.world.entity.player.ai

import com.flowpowered.nbt._
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.ai.{EntityAI, EntityAIInput}
import com.martomate.hexacraft.world.entity.player.PlayerEntity
import org.joml.{Vector3d, Vector3dc}

class SimplePlayerAI(player: PlayerEntity, input: EntityAIInput) extends EntityAI {
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
      val blockInFront = input.blockInFront(player.boundingBox.radius + speed * 4)

      if (blockInFront != Blocks.Air && player.velocity.y == 0) {
        movingForce.y = 5
      }
      val angle = player.position.angleXZ(target)

      movingForce.x = speed * math.cos(angle)
      movingForce.z = speed * math.sin(angle)
      player.rotation.y = -angle
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

  override def fromNBT(tag: CompoundTag): Unit = {
    val targetX = NBTUtil.getDouble(tag, "targetX", 0)
    val targetZ = NBTUtil.getDouble(tag, "targetZ", 0)
    target = CylCoords(targetX, 0, targetZ)(player.position.cylSize)
    timeout = NBTUtil.getShort(tag, "timeout", 0)
  }

  override def toNBT: Seq[Tag[_]] = Seq(
    new StringTag("type", "simple"),
    new DoubleTag("targetX", target.x),
    new DoubleTag("targetZ", target.z),
    new ShortTag("timeout", timeout.toShort)
  )
}
