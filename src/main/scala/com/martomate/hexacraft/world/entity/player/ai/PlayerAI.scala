package com.martomate.hexacraft.world.entity.player.ai

import org.joml.Vector3dc

abstract class PlayerAI {
  def tick(): Unit
  def acceleration(): Vector3dc
}
