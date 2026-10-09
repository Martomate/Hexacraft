package hexacraft.server.entity

import hexacraft.nbt.Nbt
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import munit.FunSuite
import org.joml.Vector3f

class EntityModelsTest extends FunSuite {
  private def makeModel(radius: Int): EntityModel = {
    EntityModel(IndexedSeq(EntityPart("body", HexPrism(radius, 8), Vector3f(), Vector3f())))
  }

  for entityType <- Seq("player", "sheep", "boat") do {
    test(s"there is a model for '$entityType'") {
      val model = EntityModels.forType(entityType).get
      assert(model.parts.exists(_.isVisible))
    }

    test(s"the model for '$entityType' survives a round trip through the codec") {
      val model = EntityModels.forType(entityType).get
      assertEquals(Nbt.decode[EntityModel](Nbt.encode(model)), Some(model))
    }

    test(s"the model ID for '$entityType' refers to the model for '$entityType'") {
      val id = EntityModels.idForType(entityType).get
      val model = EntityModels.encodedModel(id).flatMap(Nbt.decode[EntityModel])
      assertEquals(model, EntityModels.forType(entityType))
    }
  }

  test("forType and idForType return None for unknown entity types") {
    assertEquals(EntityModels.forType("unknown"), None)
    assertEquals(EntityModels.idForType("unknown"), None)
  }

  test("encodedModel returns None for an unknown ID") {
    assertEquals(EntityModels.encodedModel("unknown"), None)
  }

  test("register makes the model available by its ID") {
    val model = makeModel(1001)
    val id = EntityModels.register(model)
    assertEquals(EntityModels.encodedModel(id), Some(Nbt.encode(model)))
  }

  test("the same model always gets the same ID") {
    assertEquals(EntityModels.register(makeModel(1002)), EntityModels.register(makeModel(1002)))
  }

  test("different models get different IDs") {
    assertNotEquals(EntityModels.register(makeModel(1003)), EntityModels.register(makeModel(1004)))
  }

  test("the ID is 128 bits in hexadecimal") {
    assertEquals(EntityModels.register(makeModel(1005)), "7e83d9a6026bc06ee4e85337606c86f5")
  }
}
