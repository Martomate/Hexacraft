package hexacraft.world.entity.player

import com.flowpowered.nbt.{CompoundTag, StringTag, Tag}
import hexacraft.nbt.{Nbt, NBTUtil}
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize}
import hexacraft.world.block.{Blocks, HexBox}
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.entity.*
import hexacraft.world.entity.ai.{EntityAI, SimpleWalkAI}

class PlayerEntity(
    model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI
)(using CylinderSize)
    extends Entity(initData, model) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

  override def tick(world: BlocksInWorld, collisionDetector: CollisionDetector): Unit = {
    ai.tick(world, data, boundingBox)
    data.velocity.add(ai.acceleration())

    data.velocity.x *= 0.9
    data.velocity.z *= 0.9

    EntityPhysicsSystem(world, collisionDetector).update(data, boundingBox)
    model.tick()
  }

  override def toNBT: Nbt.MapTag =
    super.toNBT
      .withField("type", Nbt.StringTag("player"))
      .withField("ai", Nbt.from(NBTUtil.makeCompoundTag("ai", ai.toNBT)))
}

class ControlledPlayerEntity(model: EntityModel, initData: EntityBaseData) extends Entity(initData, model) {
  def setPosition(pos: CylCoords): Unit = this.data.position = pos
}
