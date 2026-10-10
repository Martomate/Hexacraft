package hexacraft.client

import hexacraft.client.entity.ModelComponent
import hexacraft.nbt.Nbt
import hexacraft.world.{CylinderSize, EntityEvent, WorldGenSettings, WorldInfo}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.{Entity, EntityModel, EntityPart, HexPrism, MountComponent}

import munit.FunSuite
import org.joml.Vector3f

import java.util.UUID
import scala.collection.mutable

class ClientWorldTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val model = EntityModel(IndexedSeq(EntityPart("body", HexPrism(16, 48), Vector3f(), Vector3f())))
  private val modelId = "sheep model"

  private def makeWorld(): ClientWorld = {
    ClientWorld(WorldInfo(2, "test world", CylinderSize(8), WorldGenSettings.fromSeed(1234)), 10)
  }

  private def spawnSheep(world: ClientWorld, modelId: Option[String]): UUID = {
    val sheep = Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "sheep").unwrap()
    val event = EntityEvent.Spawned(Entity.encode(sheep, includeAi = false), modelId)
    world.applyEntityEvents(Seq(sheep.id -> event))
    sheep.id
  }

  private def despawn(world: ClientWorld, entityId: UUID): Unit = {
    world.applyEntityEvents(Seq(entityId -> EntityEvent.Despawned))
  }

  /** Returns the entity's model, or None if it will not be rendered. Fails if the entity is not in the world. */
  private def modelOf(world: ClientWorld, entityId: UUID): Option[EntityModel] = {
    val entities = mutable.ArrayBuffer.empty[Entity]
    world.foreachEntity(entities += _)
    val entity = entities.find(_.id == entityId).getOrElse(fail("the entity is not in the world"))
    entity.accessComponent { case c: ModelComponent => c.model }
  }

  test("a spawned entity is not rendered until its model has been received") {
    val world = makeWorld()
    val sheepId = spawnSheep(world, Some(modelId))

    assertEquals(modelOf(world, sheepId), None)

    val requested = world.modelIdsToRequest()
    assertEquals(requested, Seq(modelId))

    world.receiveModels(requested, Map(modelId -> Nbt.encode(model)))
    assertEquals(modelOf(world, sheepId), Some(model))
  }

  test("a model is only requested once, even if several entities are waiting for it") {
    val world = makeWorld()
    val sheep1 = spawnSheep(world, Some(modelId))

    val requested = world.modelIdsToRequest()
    assertEquals(requested, Seq(modelId))

    val sheep2 = spawnSheep(world, Some(modelId)) // while the request is on its way
    assertEquals(world.modelIdsToRequest(), Seq())

    world.receiveModels(requested, Map(modelId -> Nbt.encode(model)))
    assertEquals(modelOf(world, sheep1), Some(model))
    assertEquals(modelOf(world, sheep2), Some(model))
  }

  test("an entity with a model that has already been received is rendered right away") {
    val world = makeWorld()
    spawnSheep(world, Some(modelId))
    world.receiveModels(world.modelIdsToRequest(), Map(modelId -> Nbt.encode(model)))

    val sheepId = spawnSheep(world, Some(modelId))

    assertEquals(modelOf(world, sheepId), Some(model))
    assertEquals(world.modelIdsToRequest(), Seq())
  }

  test("an entity whose model the server doesn't have is not rendered, and the model is not requested again") {
    val world = makeWorld()
    val sheepId = spawnSheep(world, Some(modelId))

    world.receiveModels(world.modelIdsToRequest(), Map())
    assertEquals(modelOf(world, sheepId), None)

    spawnSheep(world, Some(modelId))
    assertEquals(world.modelIdsToRequest(), Seq())
  }

  test("an entity that is removed while waiting for its model is not added back") {
    val world = makeWorld()
    val sheepId = spawnSheep(world, Some(modelId))
    despawn(world, sheepId)

    world.receiveModels(world.modelIdsToRequest(), Map(modelId -> Nbt.encode(model)))

    var entityCount = 0
    world.foreachEntity(_ => entityCount += 1)
    assertEquals(entityCount, 0)
  }

  test("an entity that is removed and added again while waiting for its model gets the model once") {
    val world = makeWorld()
    val sheep = Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "sheep").unwrap()
    val spawnEvent = EntityEvent.Spawned(Entity.encode(sheep, includeAi = false), Some(modelId))

    world.applyEntityEvents(Seq(sheep.id -> spawnEvent))
    world.applyEntityEvents(Seq(sheep.id -> EntityEvent.Despawned, sheep.id -> spawnEvent))

    world.receiveModels(world.modelIdsToRequest(), Map(modelId -> Nbt.encode(model)))

    val entities = mutable.ArrayBuffer.empty[Entity]
    world.foreachEntity(entities += _)
    assertEquals(entities.size, 1)
    assertEquals(entities.head.accessComponents { case c: ModelComponent => c }.size, 1)
  }

  test("a spawned entity without a model ID is added but not rendered") {
    val world = makeWorld()
    val sheepId = spawnSheep(world, None)

    assertEquals(modelOf(world, sheepId), None)
    assertEquals(world.modelIdsToRequest(), Seq())
  }

  test("entitiesMountedBy returns the entities the given entity is mounted on") {
    val world = makeWorld()
    val playerId = UUID.randomUUID()
    val boat = Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "boat").unwrap()
    val mountedBoat = boat.withComponent(MountComponent(playerId))
    val sheepId = spawnSheep(world, None)

    world.applyEntityEvents(Seq(boat.id -> EntityEvent.Spawned(Entity.encode(boat, includeAi = false), None)))
    assertEquals(world.entitiesMountedBy(playerId), Seq())

    // This is what the server sends when the player mounts the boat
    world.applyEntityEvents(
      Seq(
        boat.id -> EntityEvent.Despawned,
        boat.id -> EntityEvent.Spawned(Entity.encode(mountedBoat, includeAi = false), None)
      )
    )
    assertEquals(world.entitiesMountedBy(playerId).map(_.id), Seq(boat.id))
    assertEquals(world.entitiesMountedBy(sheepId), Seq())
  }
}
