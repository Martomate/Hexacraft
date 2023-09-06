package hexacraft.world.entity.ai

import hexacraft.world.BlocksInWorld
import hexacraft.world.block.HexBox
import hexacraft.world.entity.EntityBaseData

import com.flowpowered.nbt.Tag
import org.joml.Vector3dc

trait EntityAI {
  def tick(world: BlocksInWorld, entityBaseData: EntityBaseData, entityBoundingBox: HexBox): Unit
  def acceleration(): Vector3dc

  def toNBT: Seq[Tag[_]]
}