package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{Entity, EntityBaseData, EntityModel, EntityRegistry}

import munit.FunSuite

class EntitiesInChunkTest extends FunSuite {
  def make = new EntitiesInChunk

  private implicit val sizeImpl: CylinderSize = CylinderSize(4)
  class MockEntity extends Entity(new EntityBaseData(CylCoords(0, 0, 0)), null)

  test("+= should add the entity") {
    val entities = make

    assertEquals(entities.count, 0)
    entities += new MockEntity
    assertEquals(entities.count, 1)
    entities += new MockEntity
    assertEquals(entities.count, 2)
  }

  test("+= should not add duplicates") {
    val entities = make

    val ent = new MockEntity
    assertEquals(entities.count, 0)
    entities += ent
    assertEquals(entities.count, 1)
    entities += ent
    assertEquals(entities.count, 1)
  }

  test("-= should remove the entity") {
    val entities = make

    val ent = new MockEntity
    entities += ent
    entities += new MockEntity
    assertEquals(entities.count, 2)
    entities -= ent
    assertEquals(entities.count, 1)
  }

  test("-= should not remove anything if not found") {
    val entities = make

    val ent = new MockEntity
    val ent2 = new MockEntity
    entities += ent
    entities -= ent2
    assertEquals(entities.count, 1)
  }
}
