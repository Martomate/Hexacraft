package hexacraft

import hexacraft.client.EntityAppearances
import hexacraft.server.entity.EntityModels

import munit.FunSuite
import org.joml.Vector3d

/** The client tolerates part names that don't exist in the models sent by the server (since they might change), so this
  * test makes sure that the client's skins and animations actually match the server's models.
  */
class EntityAppearancesMatchModelsTest extends FunSuite {
  for entityType <- Seq("player", "sheep", "boat") do {
    test(s"the client's skin and animation for '$entityType' only refer to parts in the server's model") {
      val model = EntityModels.forType(entityType).get
      val component = EntityAppearances.modelComponent(entityType, model).get

      assertEquals(component.skin.partNames.filterNot(model.hasPart).toSet, Set.empty[String], "skin")

      component.animation.tick(true, Some(new Vector3d), new Vector3d, None)
      component.animation.tick(false, None, new Vector3d, Some(new Vector3d))
      assertEquals(component.pose.missingPartNames, Set.empty[String], "animation")
    }
  }
}
