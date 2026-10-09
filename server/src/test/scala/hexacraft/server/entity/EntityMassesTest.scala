package hexacraft.server.entity

import hexacraft.server.world.VolumeSamples

import munit.FunSuite

class EntityMassesTest extends FunSuite {
  test("the mass of a sheep is the volume of its model times its density") {
    val volume = VolumeSamples.forType("sheep").get.totalVolume
    assertEqualsDouble(EntityMasses.forType("sheep").get, volume * 950, 1e-9)
  }

  test("the mass of a boat is the volume of its model times the density of wood") {
    val volume = VolumeSamples.forType("boat").get.totalVolume
    assertEqualsDouble(EntityMasses.forType("boat").get, volume * 600, 1e-9)
  }

  test("there is no mass for unknown entity types") {
    assertEquals(EntityMasses.forType("unknown"), None)
  }
}
