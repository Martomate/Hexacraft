package hexacraft.world.entity

import hexacraft.nbt.Nbt
import hexacraft.world.CylinderSize
import hexacraft.world.coord.CylCoords

import munit.FunSuite
import org.joml.Vector3d

import java.util.UUID

class EntityTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private def makeSheepWithAi(): Entity = {
    val ai = SimpleWalkAI.create
    ai.target = CylCoords(3.5, 0, 2.25)
    ai.timeout = 42

    Entity
      .atStartPos(UUID.randomUUID(), CylCoords(1.5, 2.5, 3.5), "sheep", new Vector3d(0.1, 0.2, 0.3))
      .unwrap()
      .withComponent(AiComponent(ai))
  }

  test("atStartPos should not give the entity any AI") {
    val entity = Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "sheep").unwrap()
    assertEquals(entity.accessComponent { case c: AiComponent => c }, None)
  }

  test("atStartPos should fail for an unknown entity type") {
    assert(Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "unicorn").isErr)
  }

  test("decode should fail for an unknown entity type") {
    val tag = Nbt.makeMap("type" -> Nbt.StringTag("unicorn"))
    assertEquals(Entity.decode(tag, includeAi = true), None)
  }

  test("encode and decode should preserve the basic fields") {
    val before = makeSheepWithAi()
    before.motion.velocity.set(0.4, -0.5, 0.6)

    for includeAi <- Seq(true, false) do {
      val after = Entity.decode(Entity.encode(before, includeAi), includeAi).get

      assertEquals(after.id, before.id)
      assertEquals(after.typeName, before.typeName)
      assertEquals(after.transform.position.toVector3d, before.transform.position.toVector3d)
      assertEquals(after.transform.rotation, before.transform.rotation)
      assertEquals(after.motion.velocity, before.motion.velocity)
    }
  }

  test("encode should include the AI if includeAi is true") {
    val tag = Entity.encode(makeSheepWithAi(), includeAi = true)
    assert(tag.getMap("ai").isDefined)
  }

  test("encode should not include the AI if includeAi is false") {
    val tag = Entity.encode(makeSheepWithAi(), includeAi = false)
    assertEquals(tag.getMap("ai"), None)
  }

  test("decode should restore the AI if includeAi is true") {
    val tag = Entity.encode(makeSheepWithAi(), includeAi = true)
    val entity = Entity.decode(tag, includeAi = true).get
    val aiComponent = entity.accessComponent { case c: AiComponent => c }.get
    val ai = aiComponent.ai.asInstanceOf[SimpleWalkAI]

    assertEquals(ai.target.toVector3d, new Vector3d(3.5, 0, 2.25))
    assertEquals(ai.timeout, 42)
  }

  test("decode should ignore the AI in the data if includeAi is false") {
    val tag = Entity.encode(makeSheepWithAi(), includeAi = true)
    val entity = Entity.decode(tag, includeAi = false).get
    assertEquals(entity.accessComponent { case c: AiComponent => c }, None)
  }

  test("encode should not include mounts if there are none") {
    val entity = Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "boat").unwrap()
    assertEquals(Entity.encode(entity, includeAi = true).getList("mounts"), None)
  }

  test("encode and decode should preserve the mounts") {
    val riders = Seq(UUID.randomUUID(), UUID.randomUUID())
    val boat = riders.foldLeft(Entity.atStartPos(UUID.randomUUID(), CylCoords(0, 0, 0), "boat").unwrap()) {
      (e, rider) => e.withComponent(MountComponent(rider))
    }

    val after = Entity.decode(Entity.encode(boat, includeAi = true), includeAi = true).get
    assertEquals(after.accessComponents { case c: MountComponent => c.mountedEntity }, riders)
  }
}
