package hexacraft.server.entity

import hexacraft.nbt.Nbt
import hexacraft.world.{CylinderSize, EntityEvent, HexBox}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.*

import munit.FunSuite

import java.util.UUID

class EntitySpawnEventTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("the spawn event contains the entity (without AI) and its model") {
    val sheep = Entity
      .atStartPos(UUID.randomUUID(), CylCoords(1, 2, 3), "sheep")
      .unwrap()
      .withComponent(AiComponent(SimpleWalkAI.create))

    assertEquals(
      EntitySpawnEvent.of(sheep),
      EntityEvent.Spawned(
        Entity.encode(sheep, includeAi = false),
        Some(Nbt.encode(SheepEntityModel.model))
      )
    )
  }

  test("the spawn event has no model if the entity type has no model") {
    val components =
      Seq(TransformComponent(CylCoords(0, 0, 0)), MotionComponent(), BoundsComponent(HexBox(0.5f, 0, 0.5f)))
    val unicorn = Entity(UUID.randomUUID(), "unicorn", components)

    EntitySpawnEvent.of(unicorn) match {
      case EntityEvent.Spawned(_, model) => assertEquals(model, None)
      case e                             => fail(s"Expected a spawn event, but got $e")
    }
  }
}
