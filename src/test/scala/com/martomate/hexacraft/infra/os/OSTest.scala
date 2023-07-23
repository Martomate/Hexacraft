package com.martomate.hexacraft.infra.os

import com.martomate.hexacraft.infra.os.{Linux, Mac, Windows}
import munit.FunSuite

class OSTest extends FunSuite {
  test("name should be the name of the OS with capital initial letter") {
    assertEquals(Windows.name, "Windows")
    assertEquals(Mac.name, "Mac")
    assertEquals(Linux.name, "Linux")
  }

  test("appdataPath should be appdata for Windows") {
    val ans = System.getenv("appdata")
    assertEquals(Windows.appdataPath, ans)
  }
  test("appdataPath should be user.home for Linux") {
    val prev = System.getProperty("user.home")
    System.setProperty("user.home", "temp_path")
    try {
      assertEquals(Linux.appdataPath, "temp_path")
    } finally {
      System.setProperty("user.home", prev)
    }
  }
  test("appdataPath should be user.home for Mac") {
    val prev = System.getProperty("user.home")
    System.setProperty("user.home", "temp_path")
    try {
      assertEquals(Mac.appdataPath, "temp_path")
    } finally {
      System.setProperty("user.home", prev)
    }
  }
}
