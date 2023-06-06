package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

import munit.FunSuite
import org.lwjgl.glfw.GLFW

class VaoTest extends FunSuite {
  test("the builder does not crash") {
    OpenGL._enterTestMode()
    val vao = VAO.builder().finish(4)
  }
}
