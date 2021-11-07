package com.martomate.hexacraft.util

import java.io.File
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}

object AsyncFileIO {
  private implicit val executionContext: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

  private val lockedFiles: mutable.Set[String] = mutable.Set.empty

  def submit[T](file: File, job: File => T): Future[T] = Future {
    val realPath = file.getCanonicalPath

    lockedFiles.synchronized {
      while (lockedFiles.contains(realPath))
        lockedFiles.wait()

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

  def unload(): Unit = {
    executionContext.shutdown()
    executionContext.awaitTermination(60, TimeUnit.SECONDS)
  }
}
