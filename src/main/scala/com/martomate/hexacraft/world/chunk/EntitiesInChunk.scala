package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.{CylinderSize, NBTUtil}
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.entity.{Entity, EntityModelLoader, EntityRegistry}

import com.flowpowered.nbt.{CompoundTag, ListTag, Tag}
import scala.collection.mutable

class EntitiesInChunk {
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

object EntitiesInChunk:
  def empty: EntitiesInChunk = new EntitiesInChunk

  def fromNBT(nbt: CompoundTag)(registry: EntityRegistry)(using
      EntityModelLoader,
      CylinderSize,
      Blocks
  ): EntitiesInChunk =
    val res = new EntitiesInChunk

    for list <- NBTUtil.getList(nbt, "entities") do
      for tag <- list do
        val compTag = tag.asInstanceOf[CompoundTag]
        val entType = NBTUtil.getString(compTag, "type", "")
        registry.get(entType) match
          case Some(factory) =>
            res += factory.fromNBT(compTag)
          case None =>
            println(s"Entity-type '$entType' not found")
        res.needsToSave = true

    res
