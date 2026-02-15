package hexacraft.infra.gpu

import hexacraft.infra.gpu.{VertexAttribute, VertexBufferLayout}

import munit.FunSuite

class VertexBufferLayoutTest extends FunSuite {
  test("stride is the sum of the attribute widths") {
    val a1 = VertexAttribute(10, 3, 4, VertexAttribute.DataType.Int)
    val a2 = VertexAttribute(11, 4, 2, VertexAttribute.DataType.Float)
    assert(new VertexBufferLayout(false, Seq(a1, a2)).stride == 3 * 4 + 4 * 2)
  }

  test("builder can add int attributes") {
    assertEquals(
      VertexBufferLayout.build(
        _.ints(0, 3)
          .ints(1, 2),
        false
      ),
      VertexBufferLayout(
        false,
        Seq(
          VertexAttribute(0, 3, 4, VertexAttribute.DataType.Int),
          VertexAttribute(1, 2, 4, VertexAttribute.DataType.Int)
        )
      )
    )
  }

  test("builder can add float attributes") {
    assertEquals(
      VertexBufferLayout.build(
        _.floats(0, 3)
          .floats(1, 2),
        false
      ),
      VertexBufferLayout(
        false,
        Seq(
          VertexAttribute(0, 3, 4, VertexAttribute.DataType.Float),
          VertexAttribute(1, 2, 4, VertexAttribute.DataType.Float)
        )
      )
    )
  }

  test("builder can add multi-slot float attributes") {
    assertEquals(
      VertexBufferLayout.build(
        _.floatsArray(0, 3)(2)
          .floatsArray(2, 4)(1),
        false
      ),
      VertexBufferLayout(
        false,
        Seq(
          VertexAttribute(0, 3, 4, VertexAttribute.DataType.Float),
          VertexAttribute(1, 3, 4, VertexAttribute.DataType.Float),
          VertexAttribute(2, 4, 4, VertexAttribute.DataType.Float)
        )
      )
    )
  }
}
