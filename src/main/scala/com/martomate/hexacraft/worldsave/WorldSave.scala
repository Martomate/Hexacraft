package com.martomate.hexacraft.worldsave

import java.io.File

import com.martomate.hexacraft.util.NBTUtil

import scala.util.{Success, Try}

// This might be used in the future as a more organized alternative to the current settings system
class WorldSave private(saveDir: File)

object WorldSave {
  val LatestVersion: Short = 2

  def apply(saveDir: File): WorldSave = {
    val version = NBTUtil.getShort(NBTUtil.loadTag(new File(saveDir, "world.dat")), "version", 1)

    for (v <- version until LatestVersion)
      migrate(v, saveDir)

    new WorldSave(saveDir)
  }

  /**
    * Makes the changes needed to play on the version after the supplied one.
    * NOTE: This might be irreversible!
    */
  private def migrate(fromVersion: Int, saveDir: File): Unit = fromVersion match {
    case 1 => migrateFromV1(saveDir)
    case _ =>
  }

  private def migrateFromV1(saveDir: File): Unit = {
    val oldChunksDir = new File(saveDir, "chunks")
    if (oldChunksDir.isDirectory) {
      for (file <- oldChunksDir.listFiles()) {
        if (file.isFile) {
          val fileName = file.getName
          Try(fileName.substring(0, fileName.indexOf('.')).toLong) match {
            case Success(coords) =>
              val to = new File(saveDir, "data/" + (coords >> 12) + "/" + (coords & 0xfff) + ".dat")
              if (!to.exists()) {
                to.getParentFile.mkdirs()
                if (!file.renameTo(to)) println("Failed to move " + file.getAbsolutePath + " to " + to.getAbsolutePath)
              }
            case _ =>
          }
        }
      }
      oldChunksDir.delete()
    }
  }
}
