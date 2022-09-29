package com.martomate.hexacraft.world.chunk

import com.flowpowered.nbt.{CompoundTag, ListTag, Tag}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.entity.{Entity, EntityRegistry}

import scala.collection.mutable

class EntitiesInChunk(world: BlocksInWorld, registry: EntityRegistry) {
  private val entities: mutable.Set[Entity] = mutable.Set.empty

  var needsToSave: Boolean = false

  def +=(entity: Entity): Unit = {
    entities += entity
    needsToSave = true
  }

  def -=(entity: Entity): Unit = {
    entities -= entity
    needsToSave = true
  }

  def count: Int = entities.size

  def allEntities: Iterable[Entity] = entities

  def fromNBT(nbt: CompoundTag): Unit = NBTUtil.getList(nbt, "entities") foreach { list =>
    for (tag <- list) {
      val compTag = tag.asInstanceOf[CompoundTag]
      val entType = NBTUtil.getString(compTag, "type", "")
      registry.get(entType).map(_.createEntity(world)) match {
        case Some(ent) =>
          ent.fromNBT(compTag)
          this += ent
        case None =>
          println(s"Entity-type '$entType' not found")
      }
      needsToSave = true
    }
  }

  def toNBT: Seq[Tag[_]] = {
    Seq(
      NBTUtil.makeListTag(
        "entities",
        classOf[CompoundTag],
        entities
          .map(e => NBTUtil.makeCompoundTag("", e.toNBT))
          .toList
      )
    )
  }
}
