package hexacraft.server.world

import hexacraft.world.{CylinderSize, FakeBlocksInWorld}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.{BlockRelWorld, CylCoords}
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism}

import munit.FunSuite
import org.joml.{Vector3d, Vector3f}

class VolumeSamplesTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val px = EntityModel.pixelSize
  private val pi = math.Pi.toFloat

  private def prismVolume(radius: Int, length: Int): Double =
    1.5 * math.sqrt(3) * radius * radius * length * px * px * px

  private def uprightPrism(radius: Int, length: Int): EntityModel = {
    EntityModel(IndexedSeq(EntityPart("body", HexPrism(radius, length), Vector3f(), Vector3f())))
  }

  /** Fills the given layers (block y coordinates) with water around the origin */
  private def worldWithWater(layers: Seq[Int], metadata: Byte = 0): FakeBlocksInWorld = {
    val blocks = for {
      y <- layers
      x <- -4 to 4
      z <- -4 to 4
    } yield BlockRelWorld(x, y, z) -> BlockState(Block.Water, metadata)
    FakeBlocksInWorld.withBlocks(blocks.toMap)
  }

  // Sampling

  test("the total volume is exactly the volume of the prisms") {
    val model = EntityModel(
      IndexedSeq(
        EntityPart("a", HexPrism(8, 32), Vector3f(), Vector3f()),
        EntityPart("b", HexPrism(5, 13), Vector3f(10, 20, 30), Vector3f(0.1f, 0.2f, 0.3f))
      )
    )

    for spacing <- Seq(1.0, 3.0, 8.0, 100.0) do {
      val samples = VolumeSamples.fromModel(model, spacing)
      assertEqualsDouble(samples.totalVolume, prismVolume(8, 32) + prismVolume(5, 13), 1e-12)
    }
  }

  test("the number of samples depends on the spacing") {
    val model = uprightPrism(8, 32)

    assertEquals(VolumeSamples.fromModel(model, 8).samples.size, 6 * 1 * 1 * 4)
    assertEquals(VolumeSamples.fromModel(model, 4).samples.size, 6 * 2 * 2 * 8)
  }

  test("pivots (parts with empty prisms) have no samples") {
    val model = EntityModel(IndexedSeq(EntityPart("pivot", HexPrism.empty, Vector3f(), Vector3f())))
    assertEquals(VolumeSamples.fromModel(model).samples, IndexedSeq.empty)
  }

  test("the samples are inside the prism") {
    val model = EntityModel(
      IndexedSeq(EntityPart("body", HexPrism(8, 32), Vector3f(0, 64, 0), Vector3f(), prismOffset = -12))
    )

    for s <- VolumeSamples.fromModel(model, 2).samples do {
      assert(math.hypot(s.offset.x, s.offset.z) < 8 * px, s.offset)
      assert(s.offset.y > (64 - 12) * px && s.offset.y < (64 + 32 - 12) * px, s.offset)
    }
  }

  test("the samples follow the rotation of the part and its parent") {
    val model = EntityModel(
      IndexedSeq(
        EntityPart("pivot", HexPrism.empty, Vector3f(0, 64, 0), Vector3f(0, 0, pi / 2)),
        EntityPart("arm", HexPrism(4, 32), Vector3f(), Vector3f(), Some("pivot"))
      )
    )

    // The arm points along the y-axis of the pivot, which is rotated so that it points along the negative x-axis
    for s <- VolumeSamples.fromModel(model).samples do {
      assert(s.offset.x < 0 && s.offset.x > -32 * px, s.offset)
      assert(math.abs(s.offset.y - 64 * px) < 4 * px, s.offset)
    }
  }

  test("there are samples for the models of all entity types") {
    for entityType <- Seq("player", "sheep", "boat") do {
      assert(VolumeSamples.forType(entityType).exists(_.totalVolume > 0), entityType)
    }
    assertEquals(VolumeSamples.forType("unknown"), None)
  }

  // Volume in water

  test("the volume in water is zero if there is no water") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 32))
    val world = FakeBlocksInWorld.empty

    assertEquals(samples.volumeInWater(world, CylCoords(0, 0, 0), new Vector3d), 0.0)
  }

  test("the volume in water is the total volume if the entity is completely under water") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 32)) // 32 pixels is the height of one block
    val world = worldWithWater(Seq(0))

    assertEqualsDouble(samples.volumeInWater(world, CylCoords(0, 0, 0), new Vector3d), samples.totalVolume, 1e-12)
  }

  test("the volume in water is half the total volume if the water reaches half way up the entity") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 64)) // two blocks tall
    val world = worldWithWater(Seq(0))

    val volume = samples.volumeInWater(world, CylCoords(0, 0, 0), new Vector3d)
    assertEqualsDouble(volume, samples.totalVolume / 2, 1e-12)
  }

  test("the volume in water continues into water blocks above") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 64))
    val world = worldWithWater(Seq(0, 1))

    val volume = samples.volumeInWater(world, CylCoords(0, 0, 0), new Vector3d)
    assertEqualsDouble(volume, samples.totalVolume, 1e-12)
  }

  test("the volume in water depends on the water level of partially filled water blocks") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 32))
    val world = worldWithWater(Seq(0), metadata = 16) // half full

    val volume = samples.volumeInWater(world, CylCoords(0, 0, 0), new Vector3d)
    assertEqualsDouble(volume, samples.totalVolume / 2, 1e-12)
  }

  test("the volume in water is zero if the entity is right above the water") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 32))
    val world = worldWithWater(Seq(0))

    assertEquals(samples.volumeInWater(world, CylCoords(0, 0.5, 0), new Vector3d), 0.0)
  }

  test("the volume in water changes smoothly as the entity moves down into the water") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 64))
    val world = worldWithWater(Seq(0))

    val heights = (0 to 100).map(i => 0.5 - i * 0.005)
    val volumes = heights.map(y => samples.volumeInWater(world, CylCoords(0, y, 0), new Vector3d))

    for (v1, v2) <- volumes.zip(volumes.tail) do {
      assert(v2 >= v1, "the volume should not decrease as the entity moves down")
      assert(v2 - v1 < samples.totalVolume * 0.02, "the volume should not jump")
    }
  }

  test("the volume in water depends on the rotation of the entity") {
    val samples = VolumeSamples.fromModel(uprightPrism(8, 64))
    val world = worldWithWater(Seq(0))

    // Upright, half of the entity is under water. Lying down (rotated around the z-axis), all of it is.
    val position = CylCoords(0, 0.25, 0)
    val upright = samples.volumeInWater(world, position, new Vector3d)
    val lyingDown = samples.volumeInWater(world, position, new Vector3d(0, 0, math.Pi / 2))

    assertEqualsDouble(upright, samples.totalVolume / 4, 1e-12)
    assertEqualsDouble(lyingDown, samples.totalVolume, 1e-12)
  }
}
