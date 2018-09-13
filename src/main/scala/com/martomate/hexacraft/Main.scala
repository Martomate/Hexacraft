package com.martomate.hexacraft

import java.io.File

import com.martomate.hexacraft.util.os.OSUtils
import org.lwjgl.system.Configuration

object Main {
  def main(args: Array[String]): Unit = {
    setNatviesFolder()

    val window = new MainWindow
    window.run()
  }

  private def setNatviesFolder(): Unit = {
    var file = new File("lib/natives")
    if (!file.exists) file = new File(OSUtils.nativesPath)
    if (file.exists()) Configuration.LIBRARY_PATH.set(file.getAbsolutePath)
  }
}
