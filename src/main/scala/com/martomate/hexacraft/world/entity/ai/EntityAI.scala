package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.util.NBTSavable
import org.joml.Vector3dc

abstract class EntityAI extends NBTSavable {
  def tick(): Unit
  def acceleration(): Vector3dc
}
