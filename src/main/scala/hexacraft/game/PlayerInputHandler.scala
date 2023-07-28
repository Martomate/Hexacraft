package hexacraft.game

import hexacraft.GameKeyboard
import hexacraft.world.CollisionDetector
import hexacraft.world.player.Player

import org.joml.Vector2dc

class PlayerInputHandler(keyboard: GameKeyboard, player: Player):
  def maxSpeed: Double =
    import GameKeyboard.Key.*

    if keyboard.keyIsPressed(MoveSlowly)
    then 0.075
    else if keyboard.keyIsPressed(MoveFast)
    then 12.0
    else if keyboard.keyIsPressed(MoveSuperFast)
    then 120.0
    else 4.3

  // TODO: make Map[key: Int, state: Int] so that the game only receives key presses when it's not overlayed, or make this method not always be called
  def tick(mouseMovement: Vector2dc, maxSpeed: Double): Unit =
    import GameKeyboard.Key.*

    val rSpeed = 0.05
    if player.flying
    then player.velocity.y = 0

    val cosMove = Math.cos(player.rotation.y) * maxSpeed
    val sinMove = Math.sin(player.rotation.y) * maxSpeed

    if keyboard.keyIsPressed(MoveForward)
    then
      player.velocity.z -= cosMove
      player.velocity.x += sinMove

    if keyboard.keyIsPressed(MoveBackward)
    then
      player.velocity.z += cosMove
      player.velocity.x -= sinMove

    if keyboard.keyIsPressed(MoveRight)
    then
      player.velocity.x += cosMove
      player.velocity.z += sinMove

    if keyboard.keyIsPressed(MoveLeft)
    then
      player.velocity.x -= cosMove
      player.velocity.z -= sinMove

    if keyboard.keyIsPressed(Jump)
    then
      if player.flying
      then player.velocity.y = maxSpeed
      else if player.velocity.y == 0
      then player.velocity.y = 5

    if keyboard.keyIsPressed(Sneak)
    then
      if player.flying
      then player.velocity.y = -maxSpeed

    if keyboard.keyIsPressed(LookUp)
    then player.rotation.x -= rSpeed

    if keyboard.keyIsPressed(LookDown)
    then player.rotation.x += rSpeed

    if keyboard.keyIsPressed(LookLeft)
    then player.rotation.y -= rSpeed

    if keyboard.keyIsPressed(LookRight)
    then player.rotation.y += rSpeed

    if keyboard.keyIsPressed(TurnHeadLeft)
    then player.rotation.z -= rSpeed

    if keyboard.keyIsPressed(TurnHeadRight)
    then player.rotation.z += rSpeed

    if keyboard.keyIsPressed(ResetRotation)
    then player.rotation.set(0, 0, 0)

    player.rotation.y += mouseMovement.x * rSpeed * 0.05
    player.rotation.x -= mouseMovement.y * rSpeed * 0.05

    if player.rotation.x < -math.Pi / 2
    then player.rotation.x += (math.Pi * 2)
    else if player.rotation.x > math.Pi / 2
    then player.rotation.x -= (math.Pi * 2)

    if player.rotation.y < 0
    then player.rotation.y += (math.Pi * 2)
    else if player.rotation.y > math.Pi * 2
    then player.rotation.y -= (math.Pi * 2)

    if player.rotation.z < 0
    then player.rotation.z += (math.Pi * 2)
    else if player.rotation.z > math.Pi * 2
    then player.rotation.z -= (math.Pi * 2)
