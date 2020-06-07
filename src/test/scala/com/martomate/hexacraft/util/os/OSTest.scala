package com.martomate.hexacraft.util.os

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OSTest extends AnyFlatSpec with Matchers {
  "name" should "be the name of the OS with capital initial letter" in {
    Windows.name shouldBe "Windows"
    Mac.name shouldBe "Mac"
    Linux.name shouldBe "Linux"
  }

  "appdataPath" should "be appdata for Windows" in {
    val ans = System.getenv("appdata")
    Windows.appdataPath shouldBe ans
  }
  it should "be user.home for Linux" in {
    val prev = System.getProperty("user.home")
    System.setProperty("user.home", "temp_path")
    try {
      Linux.appdataPath shouldBe "temp_path"
    } finally {
      System.setProperty("user.home", prev)
    }
  }
  it should "be user.home for Mac" in {
    val prev = System.getProperty("user.home")
    System.setProperty("user.home", "temp_path")
    try {
      Mac.appdataPath shouldBe "temp_path"
    } finally {
      System.setProperty("user.home", prev)
    }
  }
}
