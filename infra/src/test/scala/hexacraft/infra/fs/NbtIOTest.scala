package hexacraft.infra.fs

import hexacraft.math.GzipAlgorithm
import hexacraft.nbt.Nbt

import munit.FunSuite

import java.io.*
import java.nio.file.Path
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.collection.immutable.ArraySeq

class NbtIOTest extends FunSuite {

  extension (bytes: Array[Byte])
    def gzipCompressed: Array[Byte] =
      val out = new ByteArrayOutputStream()
      val stream = new BufferedOutputStream(new GZIPOutputStream(out))
      try
        stream.write(bytes)
        stream.flush()
      finally stream.close()
      out.toByteArray

    def gzipDecompressed: Array[Byte] =
      val stream = new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))
      try
        stream.readAllBytes()
      finally stream.close()

  test("tags can be saved") {
    val fs = FileSystem.createNull()
    val tracker = fs.trackWrites()
    val nbtIO = new NbtIO(fs)

    val file = new File("abc.dat")
    val tag = Nbt.emptyMap
    nbtIO.saveTag(tag, "tag", file)

    val bytes = Array[Byte](10, 0, 3, 't', 'a', 'g', 0)
    val compressedBytes = ArraySeq.unsafeWrapArray(GzipAlgorithm.compress(bytes))

    assertEquals(tracker.events, Seq(FileSystem.FileWrittenEvent(file.toPath, compressedBytes)))
  }

  test("tags can be loaded") {
    val path = Path.of("world.dat")
    val bytes = Array[Byte](10, 0, 3, 't', 'a', 'g', 0)

    val fs = FileSystem.createNull(existingFiles = Map(path -> bytes.gzipCompressed))
    val tracker = fs.trackWrites()
    val nbtIO = new NbtIO(fs)

    val (name, tag) = nbtIO.loadTag(path.toFile).getOrElse(("", Nbt.emptyMap))

    assertEquals(tag, Nbt.emptyMap)
    assertEquals(name, "tag")
  }
}
