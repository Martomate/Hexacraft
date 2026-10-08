package hexacraft.game

import hexacraft.world.Player
import hexacraft.world.entity.Entity

import org.joml.{Vector2fc, Vector3d, Vector3dc}

class PlayerInputHandler:
  def determineMaxSpeed(pressedKeys: Seq[GameKeyboard.Key]): Double = {
    import GameKeyboard.Key.*

    if pressedKeys.contains(MoveSlowly) then {
      0.075
    } else if pressedKeys.contains(MoveFast) then {
      12.0
    } else if pressedKeys.contains(MoveSuperFast) then {
      120.0
    } else {
      4.3
    }
  }

  def tick(
      player: Player,
      pressedKeys: Seq[GameKeyboard.Key],
      mouseMovement: Vector2fc,
      maxSpeed: Double,
      isInFluid: Boolean,
      mounts: Seq[Entity]
  ): Unit = {
    if mounts.nonEmpty then {
      val mount = mounts.head
      updateMount(player, mount, pressedKeys, mouseMovement, maxSpeed, 0.02)
      updateRotation(pressedKeys, player.rotation, mouseMovement, 0.05)
      limitYawRelativeToMount(player.rotation, mount.transform.rotation, PlayerInputHandler.MaxHeadYawWhenMounted)
    } else {
      updateVelocity(pressedKeys, player.velocity, player.rotation, player.flying, maxSpeed, isInFluid)
      updateRotation(pressedKeys, player.rotation, mouseMovement, 0.05)
    }
  }

  private def updateMount(
      player: Player,
      mount: Entity,
      pressedKeys: Seq[GameKeyboard.Key],
      mouseMovement: Vector2fc,
      maxSpeed: Double,
      rSpeed: Double
  ): Unit = {
    import GameKeyboard.Key.*

    val cosMove = Math.cos(-mount.transform.rotation.y) * maxSpeed * 0.05
    val sinMove = Math.sin(-mount.transform.rotation.y) * maxSpeed * 0.05

    if pressedKeys.contains(MoveForward) then {
      mount.motion.velocity.z -= cosMove
      mount.motion.velocity.x += sinMove
    }

    if pressedKeys.contains(MoveBackward) then {
      mount.motion.velocity.z += cosMove
      mount.motion.velocity.x -= sinMove
    }

    if pressedKeys.contains(MoveRight) then {
      mount.transform.rotation.y -= rSpeed
      player.rotation.y += rSpeed
    }

    if pressedKeys.contains(MoveLeft) then {
      mount.transform.rotation.y += rSpeed
      player.rotation.y -= rSpeed
    }
  }

  /** Keeps the player looking roughly in the direction of the mount, since only the head can turn while sitting */
  private def limitYawRelativeToMount(rotation: Vector3d, mountRotation: Vector3dc, maxYaw: Double): Unit = {
    // The player faces the same way as the mount when `rotation.y == -mountRotation.y`
    val relativeYaw = PlayerInputHandler.wrapAngle(rotation.y + mountRotation.y)
    val limitedYaw = math.max(-maxYaw, math.min(maxYaw, relativeYaw))

    if limitedYaw != relativeYaw then {
      rotation.y = PlayerInputHandler.wrapAngle(limitedYaw - mountRotation.y)
      if rotation.y < 0 then {
        rotation.y += math.Pi * 2
      }
    }
  }

  private def updateVelocity(
      pressedKeys: Seq[GameKeyboard.Key],
      velocity: Vector3d,
      rotation: Vector3dc,
      isFlying: Boolean,
      maxSpeed: Double,
      isInFluid: Boolean
  ): Unit = {
    import GameKeyboard.Key.*

    if isFlying then {
      velocity.y = 0
    }

    val cosMove = Math.cos(rotation.y) * maxSpeed * 0.5
    val sinMove = Math.sin(rotation.y) * maxSpeed * 0.5

    if pressedKeys.contains(MoveForward) then {
      velocity.z -= cosMove
      velocity.x += sinMove
    }

    if pressedKeys.contains(MoveBackward) then {
      velocity.z += cosMove
      velocity.x -= sinMove
    }

    if pressedKeys.contains(MoveRight) then {
      velocity.x += cosMove
      velocity.z += sinMove
    }

    if pressedKeys.contains(MoveLeft) then {
      velocity.x -= cosMove
      velocity.z -= sinMove
    }

    if pressedKeys.contains(Jump) then {
      if isFlying then {
        velocity.y = maxSpeed
      } else if velocity.y == 0 then {
        velocity.y = 5
      } else if isInFluid then {
        velocity.y += maxSpeed * 0.04
      }
    }

    if pressedKeys.contains(Sneak) then {
      if isFlying then {
        velocity.y = -maxSpeed
      } else if isInFluid then {
        velocity.y -= maxSpeed * 0.04
      }
    }
  }

  private def updateRotation(
      pressedKeys: Seq[GameKeyboard.Key],
      rotation: Vector3d,
      mouseMovement: Vector2fc,
      rSpeed: Float
  ): Unit = {
    import GameKeyboard.Key.*

    if pressedKeys.contains(LookUp) then {
      rotation.x -= rSpeed
    }
    if pressedKeys.contains(LookDown) then {
      rotation.x += rSpeed
    }
    if pressedKeys.contains(LookLeft) then {
      rotation.y -= rSpeed
    }
    if pressedKeys.contains(LookRight) then {
      rotation.y += rSpeed
    }
    if pressedKeys.contains(TurnHeadLeft) then {
      rotation.z -= rSpeed
    }
    if pressedKeys.contains(TurnHeadRight) then {
      rotation.z += rSpeed
    }
    if pressedKeys.contains(ResetRotation) then {
      rotation.set(0, 0, 0)
    }

    rotation.y += mouseMovement.x * rSpeed * 0.05
    rotation.x -= mouseMovement.y * rSpeed * 0.05

    if rotation.x < -math.Pi / 2 then {
      rotation.x = (-math.Pi / 2).toFloat
    } else if rotation.x > math.Pi / 2 then {
      rotation.x = (math.Pi / 2).toFloat
    }

    if rotation.y < 0 then {
      rotation.y += (math.Pi * 2)
    } else if rotation.y > math.Pi * 2 then {
      rotation.y -= (math.Pi * 2)
    }

    if rotation.z < 0 then {
      rotation.z += (math.Pi * 2)
    } else if rotation.z > math.Pi * 2 then {
      rotation.z -= (math.Pi * 2)
    }
  }

object PlayerInputHandler {
  val MaxHeadYawWhenMounted: Double = math.Pi / 2

  /** Returns the given angle wrapped into the range [-pi, pi) */
  def wrapAngle(angle: Double): Double = {
    val twoPi = math.Pi * 2
    angle - twoPi * math.floor((angle + math.Pi) / twoPi)
  }
}
