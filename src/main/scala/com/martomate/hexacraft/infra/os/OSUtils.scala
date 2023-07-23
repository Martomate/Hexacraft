package com.martomate.hexacraft.infra.os

object OSUtils:
  def os: OS =
    val osStr = System.getProperty("os.name").toLowerCase()

    if osStr startsWith "win"
    then Windows
    else if (osStr startsWith "mac") || (osStr startsWith "darwin")
    then Mac
    else Linux

  def appdataPath: String = os.appdataPath
  def nativesPath: String = "lib/natives-" + os.name.toLowerCase()
