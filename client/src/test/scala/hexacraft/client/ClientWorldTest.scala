package hexacraft.client

import hexacraft.client.entity.ModelComponent
import hexacraft.nbt.Nbt
import hexacraft.world.{CylinderSize, EntityEvent, WorldGenSettings, WorldInfo}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.{Entity, EntityModel, EntityPart, HexPrism}

import munit.FunSuite
import org.joml.Vector3f

import java.util.UUID
import scala.collection.mutable

class ClientWorldTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val model = EntityModel(IndexedSeq(EntityPart("body", HexPrism(16, 48), Vector3f(), Vector3f())))

  private def makeWorld(): ClientWorld = {
    ClientWorld(WorldInfo(2, "test world", CylinderSize(8), WorldGenSettings.fromSeed(1234)), 10)
  }

  private def spawnSheep(world: ClientWorld, modelTag: Option[Nbt.MapTag]): Entity = {
    val sheep = Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "sheep").unwrap()
    val event = EntityEvent.Spawned(Entity.encode(sheep, includeAi = false), modelTag)
    world.tick(Seq.empty, Seq(sheep.id -> event))

    val entities = mutable.ArrayBuffer.empty[Entity]
    world.foreachEntity(entities += _)
    assertEquals(entities.map(_.id).toSeq, Seq(sheep.id))
    entities.head
  }

  test("a spawned entity gets the model that was sent with it") {
    val entity = spawnSheep(makeWorld(), Some(Nbt.encode(model)))

    val component = entity.accessComponent { case c: ModelComponent => c }
    assertEquals(component.map(_.model), Some(model))
  }

  test("a spawned entity without a model is added but not rendered") {
    val entity = spawnSheep(makeWorld(), None)

    assertEquals(entity.accessComponent { case c: ModelComponent => c }, None)
  }

  test("a spawned entity with an invalid model is added but not rendered") {
    val invalidModel = Nbt.makeMap("parts" -> Nbt.ListTag(Seq(Nbt.makeMap("name" -> Nbt.StringTag("no prism")))))
    val entity = spawnSheep(makeWorld(), Some(invalidModel))

    assertEquals(entity.accessComponent { case c: ModelComponent => c }, None)
  }
}
