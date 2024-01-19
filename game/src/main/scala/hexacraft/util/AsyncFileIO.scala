package hexacraft.util

import java.io.File
import scala.collection.mutable

/** This object ensures that two threads are not reading/writing the same file at the same time */
object AsyncFileIO {
  private val lockedFiles: mutable.Set[String] = mutable.Set.empty

  def perform[T](file: File, job: File => T): T = {
    val realPath = file.getCanonicalPath

    lockedFiles.synchronized {
      while lockedFiles.contains(realPath) do {
        lockedFiles.wait()
      }

      lockedFiles += realPath
    }
    try {
      job(file)
    } finally {
      lockedFiles.synchronized {
        lockedFiles -= realPath
        lockedFiles.notifyAll()
      }
    }
  }
}
