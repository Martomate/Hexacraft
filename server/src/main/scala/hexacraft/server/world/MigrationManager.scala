package hexacraft.server.world

import hexacraft.infra.fs.{FileSystem, NbtIO}

import com.martomate.nbt.Nbt

import java.io.File
import scala.util.Try

object MigrationManager {
  private val LatestVersion: Short = 2
}

class MigrationManager(fs: FileSystem) {
  // TODO: write tests for old versions by using nullable infrastructure

  def migrateIfNeeded(saveDir: File): Unit = {
    val nbtIO = new NbtIO(this.fs)
    val saveFile = new File(saveDir, "world.dat")
    val (rootName, nbtData) = nbtIO.loadTag(saveFile).getOrElse(("", Nbt.emptyMap))
    val version = nbtData.getShort("version", 1)

    if version > MigrationManager.LatestVersion then {
      throw new IllegalArgumentException(
        s"The world saved at ${saveDir.getAbsolutePath} was saved using a too new version. " +
          s"The latest supported version is ${MigrationManager.LatestVersion} but the version was $version."
      )
    }

    for v <- version.toInt until MigrationManager.LatestVersion do {
      migrateFrom(v, saveDir)
      val updatedNbt = nbtData.withField("version", Nbt.ShortTag((v + 1).toShort))
      nbtIO.saveTag(updatedNbt, rootName, saveFile)
    }
  }

  /** Upgrades the save file from version `fromVersion` to version `fromVersion + 1` <br><br>
    * <b>NOTE:</b> This might be irreversible!
    */
  private def migrateFrom(fromVersion: Int, saveDir: File): Unit = {
    fromVersion match {
      case 1 => migrateFromV1(saveDir)
      case _ =>
    }
  }

  // TODO: migrate this function to use fs instead of File
  private def migrateFromV1(saveDir: File): Unit = {
    val oldChunksDir = new File(saveDir, "chunks")
    if !oldChunksDir.isDirectory then {
      return
    }

    for file <- oldChunksDir.listFiles() if file.isFile do {
      val fileName = file.getName
      val chunkCoords = Try(fileName.substring(0, fileName.indexOf('.')).toLong).toOption

      chunkCoords match {
        case Some(coords) =>
          val to = new File(saveDir, s"data/${coords >> 12}/${coords & 0xfff}.dat")
          if !to.exists() then {
            to.getParentFile.mkdirs()
            if !file.renameTo(to) then {
              println(s"Failed to move ${file.getAbsolutePath} to ${to.getAbsolutePath}")
            }
          }
        case _ => // the file was not a chunk file, so we ignore it
      }
    }

    oldChunksDir.delete()
  }
}
