package com.martomate.hexacraft.util.os

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OSUtilsTest extends AnyFlatSpec with Matchers {
  "os" should "be Windows if os.name is Windows 10" in {
    System.setProperty("os.name", "Windows 10")
    OSUtils.os shouldBe Windows
  }
  it should "be Linux if os.name is Linux" in {
    System.setProperty("os.name", "Linux")
    OSUtils.os shouldBe Linux
  }
  it should "be Mac if os.name is Mac OS X" in {
    System.setProperty("os.name", "Mac OS X")
    OSUtils.os shouldBe Mac
  }
  it should "be Mac if os.name is macOS" in {
    System.setProperty("os.name", "macOS")
    OSUtils.os shouldBe Mac
  }
  it should "be Mac if os.name is Darwin" in {
    System.setProperty("os.name", "Darwin")
    OSUtils.os shouldBe Mac
  }
  it should "be Linux if os.name is OpenBSD" in {
    System.setProperty("os.name", "OpenBSD")
    OSUtils.os shouldBe Linux
  }
  it should "be Linux if os.name is unknown" in {
    System.setProperty("os.name", "Future OS")
    OSUtils.os shouldBe Linux
  }

}
