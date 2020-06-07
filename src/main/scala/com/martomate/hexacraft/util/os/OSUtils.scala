package com.martomate.hexacraft.util.os

object OSUtils {
  def os: OS = {
    val osStr = System.getProperty("os.name").toLowerCase()
    
    if (osStr startsWith "win") Windows
    else if ((osStr startsWith "mac") || (osStr startsWith "darwin")) Mac
    else Linux
  }
  
  def appdataPath: String = os.appdataPath
  def nativesPath: String = "lib/natives-" + os.name.toLowerCase()
}
