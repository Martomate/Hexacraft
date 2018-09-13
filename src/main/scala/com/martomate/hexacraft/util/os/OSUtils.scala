package com.martomate.hexacraft.util.os

object OSUtils {
  val os: OS = {
    val os = System.getProperty("os.name").toLowerCase()
    
    if (os contains "win") Windows
    else if (os contains "mac") Mac
    else Linux
  }
  
  def appdataPath: String = os.appdataPath
  def nativesPath: String = "lib/natives-" + os.name.toLowerCase()
}
