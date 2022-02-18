package com.martomate.hexacraft.world.gen

import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.entity.Entity

import scala.collection.mutable

class PlannedEntitySpawn {
  val entities: mutable.Buffer[Entity] = mutable.Buffer.empty

  def addEntity(entity: Entity): Unit = {
    entities += entity
  }

  def spawnEntities(chunk: Chunk): Unit = {
    for (entity <- entities) {
      chunk.entities += entity
    }
  }
}
