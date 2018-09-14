package com.martomate.hexacraft.game

import com.martomate.hexacraft.world.collision.CollisionDetector
import com.martomate.hexacraft.world.player.Player
import com.martomate.hexacraft.{GameKeyboard, GameMouse}
import org.lwjgl.glfw.GLFW._

class PlayerInputHandler(mouse: GameMouse, keyboard: GameKeyboard, val player: Player) {
  var moveWithMouse = false

  def tick(): Unit = {
    updatePlayer()
  }
  // TODO: make Map[key: Int, state: Int] so that the game only receives key presses when it's not overlayed, or make this method not always be called
  private def updatePlayer(): Unit = {
    def keyPressed(key: Int): Boolean = keyboard.getKey(key) == GLFW_PRESS
    
    val speed = if (keyPressed(GLFW_KEY_LEFT_CONTROL))    0.075
           else if (keyPressed(GLFW_KEY_LEFT_ALT))       12.0
           else if (keyPressed(GLFW_KEY_RIGHT_CONTROL)) 120.0
           else                                           4.3
    
    val rSpeed = 0.05
    if (player.flying) player.velocity.y = 0
    
    val cosMove = Math.cos(player.rotation.y) * speed
    val sinMove = Math.sin(player.rotation.y) * speed
    if (keyPressed(GLFW_KEY_W)) {
      player.velocity.z -= cosMove
      player.velocity.x += sinMove
    }
    if (keyPressed(GLFW_KEY_S)) {
      player.velocity.z += cosMove
      player.velocity.x -= sinMove
    }
    if (keyPressed(GLFW_KEY_D)) {
      player.velocity.x += cosMove
      player.velocity.z += sinMove
    }
    if (keyPressed(GLFW_KEY_A)) {
      player.velocity.x -= cosMove
      player.velocity.z -= sinMove
    }
    if (keyPressed(GLFW_KEY_SPACE)) {
      if (player.flying) player.velocity.y = speed
      else if (player.velocity.y == 0) player.velocity.y = 5
    }
    if (keyPressed(GLFW_KEY_LEFT_SHIFT)) {
      if (player.flying) player.velocity.y = -speed
    }

    if (keyPressed(GLFW_KEY_UP)         ) player.rotation.x -= rSpeed
    if (keyPressed(GLFW_KEY_DOWN)       ) player.rotation.x += rSpeed
    if (keyPressed(GLFW_KEY_LEFT)       ) player.rotation.y -= rSpeed
    if (keyPressed(GLFW_KEY_RIGHT)      ) player.rotation.y += rSpeed
    if (keyPressed(GLFW_KEY_PAGE_UP)    ) player.rotation.z -= rSpeed
    if (keyPressed(GLFW_KEY_PAGE_DOWN)  ) player.rotation.z += rSpeed

    if (moveWithMouse) {
      val mouseMoved = mouse.moved
      player.rotation.y += mouseMoved.x * rSpeed * 0.1
      player.rotation.x -= mouseMoved.y * rSpeed * 0.1
    }

    if (player.rotation.x < -math.Pi / 2) {
      player.rotation.x = -(math.Pi / 2)
    } else if (player.rotation.x > math.Pi / 2) {
      player.rotation.x = math.Pi / 2
    }

    if (player.rotation.y < 0) {
      player.rotation.y += (math.Pi * 2)
    } else if (player.rotation.y > math.Pi * 2) {
      player.rotation.y -= (math.Pi * 2)
    }

    if (player.rotation.z < 0) {
      player.rotation.z += (math.Pi * 2)
    } else if (player.rotation.z > math.Pi * 2) {
      player.rotation.z -= (math.Pi * 2)
    }

    player.velocity.x *= 0.75
    player.velocity.z *= 0.75

    val velLen = math.hypot(player.velocity.x, player.velocity.z)
    if (velLen > speed) {
      player.velocity.x *= speed / velLen
      player.velocity.z *= speed / velLen
    }
    
    if (!player.flying) {
      player.velocity.y -= 9.82 / 60
      player.velocity.div(60)
      val (pos, vel) = CollisionDetector.positionAndVelocityAfterCollision(player.bounds, player.position, player.velocity, player.world)
      player.position.set(pos)
      player.velocity.set(vel)
      player.velocity.mul(60)
    } else {
      player.position.add(player.velocity.x / 60, player.velocity.y / 60, player.velocity.z / 60)
    }
  }
}
