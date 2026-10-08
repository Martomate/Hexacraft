package hexacraft.world.entity

import hexacraft.nbt.Nbt

import munit.FunSuite
import org.joml.Vector3f

class EntityModelTest extends FunSuite {
  private val pivot = EntityPart("pivot", HexPrism.empty, Vector3f(0, 88, 0), Vector3f(0, 1.5f, 0))
  private val head =
    EntityPart("head", HexPrism(16, 20), Vector3f(1.5f, 13.86f, -2), Vector3f(0.1f, 0.2f, 0.3f), Some("pivot"), -10)
  private val model = EntityModel(IndexedSeq(pivot, head))

  private def partTag(fields: (String, Nbt)*): Nbt.MapTag = {
    Nbt.makeMap(fields*)
  }

  private def prismTag(radius: Int, length: Int): Nbt.MapTag = {
    Nbt.makeMap("radius" -> Nbt.IntTag(radius), "length" -> Nbt.IntTag(length))
  }

  private def modelTag(parts: Nbt.MapTag*): Nbt.MapTag = {
    Nbt.makeMap("parts" -> Nbt.ListTag(parts))
  }

  test("encode and decode should preserve all fields of all parts") {
    assertEquals(Nbt.decode[EntityModel](Nbt.encode(model)), Some(model))
  }

  test("the model should survive a round trip through the binary format") {
    val bytes = Nbt.encode(model).toBinary()
    val (_, tag) = Nbt.fromBinary(bytes)
    assertEquals(Nbt.decode[EntityModel](tag.asMap.get), Some(model))
  }

  test("encode should not include the parent of a part without a parent") {
    val tag = Nbt.encode(pivot)
    assertEquals(tag.getTag("parent"), None)
  }

  test("decode should use zero for a missing position, rotation and prism offset") {
    val tag = partTag("name" -> Nbt.StringTag("leg"), "prism" -> prismTag(4, 32))
    val expected = EntityPart("leg", HexPrism(4, 32), Vector3f(), Vector3f(), None, 0)
    assertEquals(Nbt.decode[EntityPart](tag), Some(expected))
  }

  test("decode should fail for a part without a name") {
    val tag = partTag("prism" -> prismTag(4, 32))
    assertEquals(Nbt.decode[EntityPart](tag), None)
  }

  test("decode should fail for a part without a prism") {
    val tag = partTag("name" -> Nbt.StringTag("leg"))
    assertEquals(Nbt.decode[EntityPart](tag), None)
  }

  test("decode should fail for a prism with sizes that are not integers") {
    val prism = Nbt.makeMap("radius" -> Nbt.FloatTag(4), "length" -> Nbt.IntTag(32))
    val tag = partTag("name" -> Nbt.StringTag("leg"), "prism" -> prism)
    assertEquals(Nbt.decode[EntityPart](tag), None)
  }

  test("decode should fail for a model without a list of parts") {
    assertEquals(Nbt.decode[EntityModel](Nbt.emptyMap), None)
  }

  test("decode should fail for a model with an invalid part") {
    val validPart = partTag("name" -> Nbt.StringTag("leg"), "prism" -> prismTag(4, 32))
    val invalidPart = partTag("name" -> Nbt.StringTag("arm"))
    assertEquals(Nbt.decode[EntityModel](modelTag(validPart, invalidPart)), None)
  }

  test("decode should fail for a model with a parent that is not in the model") {
    val part = partTag("name" -> Nbt.StringTag("head"), "prism" -> prismTag(8, 16), "parent" -> Nbt.StringTag("pivot"))
    assertEquals(Nbt.decode[EntityModel](modelTag(part)), None)
  }

  test("decode should fail for a model where a parent comes after its child") {
    val child = partTag("name" -> Nbt.StringTag("head"), "prism" -> prismTag(8, 16), "parent" -> Nbt.StringTag("pivot"))
    val parent = partTag("name" -> Nbt.StringTag("pivot"), "prism" -> prismTag(0, 0))
    assertEquals(Nbt.decode[EntityModel](modelTag(child, parent)), None)
  }

  test("decode should fail for a model with duplicate part names") {
    val part = partTag("name" -> Nbt.StringTag("leg"), "prism" -> prismTag(4, 32))
    assertEquals(Nbt.decode[EntityModel](modelTag(part, part)), None)
  }
}
