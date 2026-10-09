package hexacraft.client

import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import munit.FunSuite
import org.joml.{Vector3d, Vector3f}

class EntityAppearancesTest extends FunSuite {

  /** A model that has none of the parts that the skins and animations refer to */
  private val unexpectedModel = EntityModel(IndexedSeq(EntityPart("blob", HexPrism(8, 8), Vector3f(), Vector3f())))

  for entityType <- Seq("player", "sheep", "boat") do {
    test(s"the appearance of '$entityType' can be used with a model that lacks the expected parts") {
      val component = EntityAppearances.modelComponent(entityType, unexpectedModel).get

      assertEquals(component.model, unexpectedModel)

      component.animation.tick(true, Some(new Vector3d), new Vector3d, None)
      component.animation.tick(false, None, new Vector3d, Some(new Vector3d))

      val part = unexpectedModel.parts.head
      component.skin.textureOffset(part, 0)
      component.skin.textureSize(part, 0)
    }
  }

  test("modelComponent returns None for unknown entity types") {
    assertEquals(EntityAppearances.modelComponent("unknown", unexpectedModel), None)
  }
}
