package hexacraft.server.entity

import hexacraft.physics.Density
import hexacraft.server.world.VolumeSamples

import munit.FunSuite

class EntityMassesTest extends FunSuite {
  for entityType <- Seq("sheep", "boat") do {
    test(s"the mass of '$entityType' is the volume of its model times its density") {
      val volume = VolumeSamples.forType(entityType).get.totalVolume
      val density = EntityMasses.densityForType(entityType).get
      assertEqualsDouble(EntityMasses.forType(entityType).get, volume * density.toSI, 1e-9)
    }

    test(s"'$entityType' is less dense than water, so it floats") {
      assert(EntityMasses.densityForType(entityType).get.toSI < Density.water.toSI)
    }
  }

  test("there is no mass for unknown entity types") {
    assertEquals(EntityMasses.forType("unknown"), None)
    assertEquals(EntityMasses.densityForType("unknown"), None)
  }
}
