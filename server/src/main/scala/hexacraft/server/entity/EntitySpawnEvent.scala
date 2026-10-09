package hexacraft.server.entity

import hexacraft.world.EntityEvent
import hexacraft.world.entity.Entity

object EntitySpawnEvent {

  /** The event that tells a client about an entity, including the ID of the entity's model so the client can fetch
    * the model and render the entity
    */
  def of(entity: Entity): EntityEvent = {
    EntityEvent.Spawned(
      Entity.encode(entity, includeAi = false),
      EntityModels.idForType(entity.typeName)
    )
  }
}
