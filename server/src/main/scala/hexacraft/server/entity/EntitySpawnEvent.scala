package hexacraft.server.entity

import hexacraft.nbt.Nbt
import hexacraft.world.EntityEvent
import hexacraft.world.entity.Entity

object EntitySpawnEvent {

  /** The event that tells a client about an entity, including the entity's model so the client can render it */
  def of(entity: Entity): EntityEvent = {
    EntityEvent.Spawned(
      Entity.encode(entity, includeAi = false),
      EntityModels.forType(entity.typeName).map(Nbt.encode(_))
    )
  }
}
