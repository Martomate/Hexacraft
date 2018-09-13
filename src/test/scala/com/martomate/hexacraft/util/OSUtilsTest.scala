package com.martomate.hexacraft.util

import com.martomate.hexacraft.util.os.{Linux, Mac, Windows}
import org.scalatest.{FlatSpec, Matchers}

class OSUtilsTest extends FlatSpec with Matchers {
  "name" should "be the name of the OS with capital initial letter" in {
    Windows.name shouldBe "Windows"
    Mac.name shouldBe "Mac"
    Linux.name shouldBe "Linux"
  }
}
