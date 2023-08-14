package hexacraft.world.entity.sheep

import hexacraft.nbt.{Nbt, NBTUtil}
import hexacraft.world.{BlocksInWorld, CollisionDetector, CylinderSize}
import hexacraft.world.block.HexBox
import hexacraft.world.entity.*
import hexacraft.world.entity.ai.EntityAI

class SheepEntity(
    model: EntityModel,
    initData: EntityBaseData,
    private val ai: EntityAI
)(using CylinderSize)
    extends Entity(initData, model) {
  override val boundingBox: HexBox = new HexBox(0.4f, 0, 0.75f)

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
      .withField("type", Nbt.StringTag("sheep"))
      .withField("ai", Nbt.from(NBTUtil.makeCompoundTag("ai", ai.toNBT)))
}
