package hexacraft.game

import hexacraft.world.Player

import org.joml.{Vector2fc, Vector3d, Vector3dc}

class PlayerInputHandler(keyboard: GameKeyboard):
  def determineMaxSpeed: Double = {
    import GameKeyboard.Key.*

    if keyboard.keyIsPressed(MoveSlowly) then {
      0.075
    } else if keyboard.keyIsPressed(MoveFast) then {
      12.0
    } else if keyboard.keyIsPressed(MoveSuperFast) then {
      120.0
    } else {
      4.3
    }
  }

  def tick(player: Player, mouseMovement: Vector2fc, maxSpeed: Double, isInFluid: Boolean): Unit = {
    updateVelocity(player.velocity, player.rotation, player.flying, maxSpeed, isInFluid)
    updateRotation(player.rotation, mouseMovement, 0.05)
  }

  private def updateVelocity(
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

    if keyboard.keyIsPressed(MoveForward) then {
      velocity.z -= cosMove
      velocity.x += sinMove
    }

    if keyboard.keyIsPressed(MoveBackward) then {
      velocity.z += cosMove
      velocity.x -= sinMove
    }

    if keyboard.keyIsPressed(MoveRight) then {
      velocity.x += cosMove
      velocity.z += sinMove
    }

    if keyboard.keyIsPressed(MoveLeft) then {
      velocity.x -= cosMove
      velocity.z -= sinMove
    }

    if keyboard.keyIsPressed(Jump) then {
      if isFlying then {
        velocity.y = maxSpeed
      } else if velocity.y == 0 then {
        velocity.y = 5
      } else if isInFluid then {
        velocity.y += maxSpeed * 0.04
      }
    }

    if keyboard.keyIsPressed(Sneak) then {
      if isFlying then {
        velocity.y = -maxSpeed
      } else if isInFluid then {
        velocity.y -= maxSpeed * 0.04
      }
    }
  }

  private def updateRotation(rotation: Vector3d, mouseMovement: Vector2fc, rSpeed: Float): Unit = {
    import GameKeyboard.Key.*

    if keyboard.keyIsPressed(LookUp) then {
      rotation.x -= rSpeed
    }
    if keyboard.keyIsPressed(LookDown) then {
      rotation.x += rSpeed
    }
    if keyboard.keyIsPressed(LookLeft) then {
      rotation.y -= rSpeed
    }
    if keyboard.keyIsPressed(LookRight) then {
      rotation.y += rSpeed
    }
    if keyboard.keyIsPressed(TurnHeadLeft) then {
      rotation.z -= rSpeed
    }
    if keyboard.keyIsPressed(TurnHeadRight) then {
      rotation.z += rSpeed
    }
    if keyboard.keyIsPressed(ResetRotation) then {
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
