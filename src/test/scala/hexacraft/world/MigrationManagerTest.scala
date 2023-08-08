package hexacraft.world

import hexacraft.infra.fs.FileSystem
import hexacraft.nbt.Nbt
import hexacraft.util.GzipAlgorithm

import munit.FunSuite

import java.nio.file.Path
import scala.collection.immutable.ArraySeq

class MigrationManagerTest extends FunSuite {
  extension (bytes: Array[Byte]) def asHexString: String = bytes.map(b => "%02X".format(b)).mkString("")

  // TODO: change the following behaviour to be fail-fast
  test("migrateIfNeeded creates a new world file if it does not exist".ignore) {
    val dir = Path.of("test_world")

    val fs = FileSystem.createNull()
    val tracker = fs.trackWrites()

    new MigrationManager(fs).migrateIfNeeded(dir.toFile)

    val expectedWorldTag = Nbt.makeMap("version" -> Nbt.ShortTag(2))

    assertEquals(
      tracker.events,
      Seq(
        FileSystem.FileWrittenEvent(
          dir.resolve("world.dat"),
          ArraySeq.unsafeWrapArray(GzipAlgorithm.compress(expectedWorldTag.toBinary()))
        )
      )
    )
  }

  test("migrateIfNeeded does nothing if the world save is in version 2".ignore) {
    val dir = Path.of("test_world")
    val saveFile = dir.resolve("world.dat")
    val worldTag = Nbt.makeMap("version" -> Nbt.ShortTag(2), "abc" -> Nbt.LongTag(123))

    val fs = FileSystem.createNull(existingFiles = Map(saveFile -> GzipAlgorithm.compress(worldTag.toBinary())))
    val tracker = fs.trackWrites()

    new MigrationManager(fs).migrateIfNeeded(dir.toFile)

    assertEquals(tracker.events, Seq())
  }

  test("migrateIfNeeded migrates the world file from version 1 to version 2".ignore) {
    val dir = Path.of("test_world")
    val saveFile = dir.resolve("world.dat")
    val existingWorldTag = Nbt.makeMap("version" -> Nbt.ShortTag(1), "abc" -> Nbt.LongTag(123))
    val expectedWorldTag = Nbt.makeMap("version" -> Nbt.ShortTag(2), "abc" -> Nbt.LongTag(123))

    val fs =
      FileSystem.createNull(existingFiles = Map(saveFile -> GzipAlgorithm.compress(existingWorldTag.toBinary())))
    val tracker = fs.trackWrites()

    new MigrationManager(fs).migrateIfNeeded(dir.toFile)

    assertEquals(
      tracker.events,
      Seq(
        FileSystem.FileWrittenEvent(
          saveFile,
          ArraySeq.unsafeWrapArray(GzipAlgorithm.compress(expectedWorldTag.toBinary()))
        )
      )
    )
  }
}
