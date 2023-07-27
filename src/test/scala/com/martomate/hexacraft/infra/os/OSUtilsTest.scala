package com.martomate.hexacraft.infra.os

import com.martomate.hexacraft.infra.os.{Linux, Mac, OSUtils, Windows}

import munit.FunSuite

class OSUtilsTest extends FunSuite {
  test("os should be Windows if os.name is Windows 10") {
    System.setProperty("os.name", "Windows 10")
    assertEquals(OSUtils.os, Windows)
  }
  test("os should be Linux if os.name is Linux") {
    System.setProperty("os.name", "Linux")
    assertEquals(OSUtils.os, Linux)
  }
  test("os should be Mac if os.name is Mac OS X") {
    System.setProperty("os.name", "Mac OS X")
    assertEquals(OSUtils.os, Mac)
  }
  test("os should be Mac if os.name is macOS") {
    System.setProperty("os.name", "macOS")
    assertEquals(OSUtils.os, Mac)
  }
  test("os should be Mac if os.name is Darwin") {
    System.setProperty("os.name", "Darwin")
    assertEquals(OSUtils.os, Mac)
  }
  test("os should be Linux if os.name is OpenBSD") {
    System.setProperty("os.name", "OpenBSD")
    assertEquals(OSUtils.os, Linux)
  }
  test("os should be Linux if os.name is unknown") {
    System.setProperty("os.name", "Future OS")
    assertEquals(OSUtils.os, Linux)
  }
}
