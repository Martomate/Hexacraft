package com.martomate.hexacraft.renderer

import com.martomate.hexacraft.util.OpenGL

import munit.FunSuite

class VboLayoutTest extends FunSuite {
  test("stride is the sum of the attribute widths") {
    val a1 = VboAttribute(10, 3, 4, VboAttribute.Format.Int)
    val a2 = VboAttribute(11, 4, 2, VboAttribute.Format.Float)
    assert(new VboLayout(Seq(a1, a2)).stride == 3 * 4 + 4 * 2)
  }

  test("builder can add int attributes") {
    assertEquals(
      VboLayout.builder().ints(0, 3).ints(1, 2).build(),
      VboLayout(
        Seq(
          VboAttribute(0, 3, 4, VboAttribute.Format.Int),
          VboAttribute(1, 2, 4, VboAttribute.Format.Int)
        )
      )
    )
  }

  test("builder can add float attributes") {
    assertEquals(
      VboLayout.builder().floats(0, 3).floats(1, 2).build(),
      VboLayout(
        Seq(
          VboAttribute(0, 3, 4, VboAttribute.Format.Float),
          VboAttribute(1, 2, 4, VboAttribute.Format.Float)
        )
      )
    )
  }

  test("builder can add multi-slot float attributes") {
    assertEquals(
      VboLayout.builder().floatsArray(0, 3)(2).floatsArray(2, 4)(1).build(),
      VboLayout(
        Seq(
          VboAttribute(0, 3, 4, VboAttribute.Format.Float),
          VboAttribute(1, 3, 4, VboAttribute.Format.Float),
          VboAttribute(2, 4, 4, VboAttribute.Format.Float)
        )
      )
    )
  }
}
