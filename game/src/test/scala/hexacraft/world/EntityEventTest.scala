package hexacraft.world

import hexacraft.nbt.Nbt

import munit.FunSuite

class EntityEventTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val data = Nbt.makeMap("type" -> Nbt.StringTag("sheep"))

  test("a spawn event with a model ID survives a round trip through the codec") {
    val event = EntityEvent.Spawned(data, Some("0123456789abcdef"))
    assertEquals(Nbt.decode[EntityEvent](Nbt.encode(event)), Some(event))
  }

  test("a spawn event without a model ID survives a round trip through the codec") {
    val event = EntityEvent.Spawned(data, None)
    assertEquals(Nbt.decode[EntityEvent](Nbt.encode(event)), Some(event))
  }

  test("a spawn event without a model ID is encoded without the model ID field") {
    val tag = Nbt.encode(EntityEvent.Spawned(data, None))
    assertEquals(tag.getTag("model_id"), None)
  }
}
