package com.martomate.hexacraft.world

import com.flowpowered.nbt.{CompoundTag, ListTag, Tag}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.chunk.EntitiesInChunk
import com.martomate.hexacraft.world.entity.Entity
import com.martomate.hexacraft.world.entity.registry.EntityRegistry
import com.martomate.hexacraft.world.worldlike.IWorld

import scala.collection.mutable

class EntitiesInChunkImpl(world: IWorld) extends EntitiesInChunk {
  private val entities: mutable.Set[Entity] = mutable.Set.empty

  var needsToSave: Boolean = false

  override def +=(entity: Entity): Unit = {
    entities += entity
    needsToSave = true
  }

  override def -=(entity: Entity): Unit = {
    entities -= entity
    needsToSave = true
  }

  override def count: Int = entities.size

  override def allEntities: Iterable[Entity] = entities

  override def fromNBT(nbt: CompoundTag): Unit = NBTUtil.getList(nbt, "entities") foreach { list =>
    for (tag <- list) {
      val compTag = tag.asInstanceOf[CompoundTag]
      val entType = NBTUtil.getString(compTag, "type", "")
      EntityRegistry.load(entType, world) match {
        case Some(ent) =>
          ent.fromNBT(compTag)
          this += ent
        case None =>
          println(s"Entity-type '$entType' not found")
      }
      needsToSave = true
    }
  }

  override def toNBT: Seq[Tag[_]] = {
    import scala.jdk.CollectionConverters._
    Seq(new ListTag[CompoundTag](
      "entities",
      classOf[CompoundTag],
      entities
        .map(e => NBTUtil.makeCompoundTag("", e.toNBT))
        .toList
        .asJava
      ))
  }
}
