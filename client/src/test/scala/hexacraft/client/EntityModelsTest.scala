package hexacraft.client

import hexacraft.nbt.Nbt
import hexacraft.world.entity.EntityModel

import munit.FunSuite
import org.joml.Vector3d

class EntityModelsTest extends FunSuite {
  for entityType <- Seq("player", "sheep", "boat") do {
    test(s"the model and animation for '$entityType' can be created and ticked") {
      val component = EntityModels.forType(entityType).get

      assert(component.model.parts.exists(_.isVisible))

      component.animation.tick(true, Some(new Vector3d), new Vector3d, None)
      component.animation.tick(false, None, new Vector3d, Some(new Vector3d))
    }

    test(s"the model for '$entityType' survives a round trip through the codec") {
      val model = EntityModels.forType(entityType).get.model

      assertEquals(Nbt.decode[EntityModel](Nbt.encode(model)), Some(model))
    }
  }

  test("forType returns None for unknown entity types") {
    assertEquals(EntityModels.forType("unknown"), None)
  }
}
