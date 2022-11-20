package com.martomate.hexacraft.world.chunk

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.entity.{Entity, EntityModel, EntityRegistry}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EntitiesInChunkTest extends AnyFlatSpec with Matchers {
  def make = new EntitiesInChunk

  private implicit val sizeImpl: CylinderSize = CylinderSize(4)
  class MockEntity extends Entity {
    override def model: EntityModel = ???

    override def id: String = "TestEntity"
  }

  "+=" should "add the entity" in {
    val entities = make

    entities.count shouldBe 0
    entities += new MockEntity
    entities.count shouldBe 1
    entities += new MockEntity
    entities.count shouldBe 2
  }

  it should "not add duplicates" in {
    val entities = make

    val ent = new MockEntity
    entities.count shouldBe 0
    entities += ent
    entities.count shouldBe 1
    entities += ent
    entities.count shouldBe 1
  }

  "-=" should "remove the entity" in {
    val entities = make

    val ent = new MockEntity
    entities += ent
    entities += new MockEntity
    entities.count shouldBe 2
    entities -= ent
    entities.count shouldBe 1
  }

  it should "not remove anything if not found" in {
    val entities = make

    val ent = new MockEntity
    val ent2 = new MockEntity
    entities += ent
    entities -= ent2
    entities.count shouldBe 1
  }
}
