package hexacraft.server.entity

import hexacraft.nbt.Nbt
import hexacraft.world.entity.EntityModel

import munit.FunSuite

class EntityModelsTest extends FunSuite {
  for entityType <- Seq("player", "sheep", "boat") do {
    test(s"there is a model for '$entityType'") {
      val model = EntityModels.forType(entityType).get
      assert(model.parts.exists(_.isVisible))
    }

    test(s"the model for '$entityType' survives a round trip through the codec") {
      val model = EntityModels.forType(entityType).get
      assertEquals(Nbt.decode[EntityModel](Nbt.encode(model)), Some(model))
    }
  }

  test("forType returns None for unknown entity types") {
    assertEquals(EntityModels.forType("unknown"), None)
  }
}
