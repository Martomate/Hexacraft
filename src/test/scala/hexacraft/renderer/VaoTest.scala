package hexacraft.renderer

import hexacraft.infra.gpu.OpenGL

import munit.FunSuite

class VaoTest extends FunSuite {
  test("the builder does not crash") {
    OpenGL._enterTestMode()
    val vao = VAO.builder().finish(4)
  }
}
