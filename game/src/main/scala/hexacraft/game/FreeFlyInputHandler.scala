package hexacraft.game

import org.joml.{Vector2fc, Vector3d, Vector3f}

class FreeFlyInputHandler {
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

  def calculateVelocity(pressedKeys: Seq[GameKeyboard.Key], rotation: Vector3f): Vector3d = {
    import GameKeyboard.Key.*

    val maxSpeed = determineMaxSpeed(pressedKeys)
    val cosMove = Math.cos(rotation.y) * maxSpeed
    val sinMove = Math.sin(rotation.y) * maxSpeed

    val velocity: Vector3d = Vector3d(0)

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
      velocity.y += maxSpeed
    }

    if pressedKeys.contains(Sneak) then {
      velocity.y -= maxSpeed
    }

    velocity
  }

  def updateRotation(
      pressedKeys: Seq[GameKeyboard.Key],
      rotation: Vector3f,
      mouseMovement: Vector2fc
  ): Unit = {
    import GameKeyboard.Key.*

    val rSpeed: Float = 0.05

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

    rotation.y += mouseMovement.x * rSpeed * 0.05f
    rotation.x -= mouseMovement.y * rSpeed * 0.05f

    if rotation.x < -math.Pi / 2 then {
      rotation.x = (-math.Pi / 2).toFloat
    } else if rotation.x > math.Pi / 2 then {
      rotation.x = (math.Pi / 2).toFloat
    }

    if rotation.y < 0 then {
      rotation.y += (math.Pi * 2).toFloat
    } else if rotation.y > math.Pi * 2 then {
      rotation.y -= (math.Pi * 2).toFloat
    }

    if rotation.z < 0 then {
      rotation.z += (math.Pi * 2).toFloat
    } else if rotation.z > math.Pi * 2 then {
      rotation.z -= (math.Pi * 2).toFloat
    }
  }
}
