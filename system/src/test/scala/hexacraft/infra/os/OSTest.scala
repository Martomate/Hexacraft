package hexacraft.infra.os

import munit.FunSuite

class OSTest extends FunSuite {
  test("OS is determined from the os.name system property") {
    testOsName("Windows 10", Windows)

    testOsName("Mac OS X", Mac)
    testOsName("macOS", Mac)
    testOsName("Darwin", Mac)

    testOsName("Linux", Linux)
    testOsName("OpenBSD", Linux)
    testOsName("Future OS", Linux)
  }

  test("OS name is capitalized") {
    assertEquals(Windows.name, "Windows")
    assertEquals(Mac.name, "Mac")
    assertEquals(Linux.name, "Linux")
  }

  test("appdataPath is the appdata env on Windows") {
    val ans = System.getenv("appdata")
    assertEquals(Windows.appdataPath, ans)
  }

  test("appdataPath is user.home property on Linux") {
    withSystemProperty("user.home", "temp_path") {
      assertEquals(Linux.appdataPath, "temp_path")
    }
  }

  test("appdataPath is user.home property on Mac") {
    withSystemProperty("user.home", "temp_path") {
      assertEquals(Mac.appdataPath, "temp_path")
    }
  }

  private def testOsName(osName: String, os: OS)(using munit.Location): Unit = {
    withSystemProperty("os.name", osName) {
      assertEquals(OSUtils.os, os)
    }
  }

  private def withSystemProperty(name: String, value: String)(run: => Any): Unit = {
    val oldValue = System.getProperty(name)
    System.setProperty(name, value)
    try {
      run
    } finally {
      System.setProperty(name, oldValue)
    }
  }
}
