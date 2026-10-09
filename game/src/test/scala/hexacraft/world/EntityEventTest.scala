package hexacraft.world

import hexacraft.nbt.Nbt

import munit.FunSuite

class EntityEventTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val data = Nbt.makeMap("type" -> Nbt.StringTag("sheep"))
  private val model = Nbt.makeMap("parts" -> Nbt.ListTag(Seq()))

  test("a spawn event with a model survives a round trip through the codec") {
    val event = EntityEvent.Spawned(data, Some(model))
    assertEquals(Nbt.decode[EntityEvent](Nbt.encode(event)), Some(event))
  }

  test("a spawn event without a model survives a round trip through the codec") {
    val event = EntityEvent.Spawned(data, None)
    assertEquals(Nbt.decode[EntityEvent](Nbt.encode(event)), Some(event))
  }

  test("a spawn event without a model is encoded without the model field") {
    val tag = Nbt.encode(EntityEvent.Spawned(data, None))
    assertEquals(tag.getTag("model"), None)
  }
}
