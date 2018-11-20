package com.martomate.hexacraft.world

import com.flowpowered.nbt.{CompoundTag, ListTag, Tag}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.chunk.EntitiesInChunk
import com.martomate.hexacraft.world.entity.{Entity, EntityLoader}

import scala.collection.JavaConverters._
import scala.collection.mutable

class EntitiesInChunkImpl extends EntitiesInChunk {
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
      EntityLoader.load(entType) match {
        case Some(ent) =>
          ent.fromNBT(compTag)
          +=(ent)
        case None =>
          println(s"Entity-type '$entType' not found")
      }
      needsToSave = true
    }
  }

  override def toNBT: Seq[Tag[_]] = Seq(new ListTag[CompoundTag]("entities", classOf[CompoundTag], entities.map(e => NBTUtil.makeCompoundTag("", e.toNBT)).toList.asJava))
}
