package hexacraft.client

import hexacraft.client.EntityModelCache.Lookup
import hexacraft.nbt.Nbt
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import munit.FunSuite
import org.joml.Vector3f

class EntityModelCacheTest extends FunSuite {
  private val model = EntityModel(IndexedSeq(EntityPart("body", HexPrism(16, 48), Vector3f(), Vector3f())))

  test("an unknown model is pending and will be requested") {
    val cache = EntityModelCache()

    assertEquals(cache.lookup("a"), Lookup.Pending)
    assertEquals(cache.takeIdsToRequest(), Seq("a"))
  }

  test("a model is only requested once even if it's looked up many times") {
    val cache = EntityModelCache()

    cache.lookup("a")
    cache.lookup("a")
    assertEquals(cache.takeIdsToRequest(), Seq("a"))

    cache.lookup("a") // while the request is on its way
    assertEquals(cache.takeIdsToRequest(), Seq())
  }

  test("a received model is available, and is not requested again") {
    val cache = EntityModelCache()

    cache.lookup("a")
    cache.receive(cache.takeIdsToRequest(), Map("a" -> Nbt.encode(model)))

    assertEquals(cache.lookup("a"), Lookup.Available(model))
    assertEquals(cache.takeIdsToRequest(), Seq())
  }

  test("a model the server doesn't have is unavailable, and is not requested again") {
    val cache = EntityModelCache()

    cache.lookup("a")
    cache.receive(cache.takeIdsToRequest(), Map())

    assertEquals(cache.lookup("a"), Lookup.Unavailable)
    assertEquals(cache.takeIdsToRequest(), Seq())
  }

  test("an invalid model is unavailable") {
    val cache = EntityModelCache()

    cache.lookup("a")
    cache.receive(cache.takeIdsToRequest(), Map("a" -> Nbt.emptyMap))

    assertEquals(cache.lookup("a"), Lookup.Unavailable)
  }

  test("only the requested IDs are handled when receiving models") {
    val cache = EntityModelCache()

    cache.lookup("a")
    cache.receive(cache.takeIdsToRequest(), Map("a" -> Nbt.encode(model), "b" -> Nbt.encode(model)))

    assertEquals(cache.lookup("b"), Lookup.Pending)
  }
}
