package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.infra.gpu.OpenGL
import munit.FunSuite

class VaoTest extends FunSuite {
  test("the builder does not crash") {
    OpenGL._enterTestMode()
    val vao = VAO.builder().finish(4)
  }
}
