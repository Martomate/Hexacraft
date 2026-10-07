package hexacraft.server.world

import hexacraft.world.CylinderSize
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.{Entity, SimpleWalkAI}

import munit.FunSuite

class EntityFactoryTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  test("a sheep should get a SimpleWalkAI") {
    val sheep = EntityFactory.atStartPos(Entity.getNextId, CylCoords(0, 0, 0), "sheep").unwrap()
    assert(sheep.ai.exists(_.isInstanceOf[SimpleWalkAI]))
  }

  test("a boat should not get any AI") {
    val boat = EntityFactory.atStartPos(Entity.getNextId, CylCoords(0, 0, 0), "boat").unwrap()
    assertEquals(boat.ai, None)
  }

  test("an unknown entity type should fail") {
    assert(EntityFactory.atStartPos(Entity.getNextId, CylCoords(0, 0, 0), "unicorn").isErr)
  }
}
