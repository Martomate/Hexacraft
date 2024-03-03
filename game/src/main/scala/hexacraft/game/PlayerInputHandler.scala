package hexacraft.game

import hexacraft.world.Player

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
      isInFluid: Boolean
  ): Unit = {
    updateVelocity(pressedKeys, player.velocity, player.rotation, player.flying, maxSpeed, isInFluid)
    updateRotation(pressedKeys, player.rotation, mouseMovement, 0.05)
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
