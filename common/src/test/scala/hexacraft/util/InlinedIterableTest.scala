package hexacraft.util

import munit.FunSuite

class InlinedIterableTest extends FunSuite {
  test("InlinedIterable should work in a for loop") {
    val items = InlinedIterable(Seq(1, 2, 3))

    var acc = 0
    for item <- items do {
      acc += item
    }

    assertEquals(acc, 1 + 2 + 3)
  }

  test("InlinedIterable should work in a for loop with filter") {
    val items = InlinedIterable(Seq(1, 2, 3))

    var acc = 0
    for item <- items if item != 2 do {
      acc += item
    }

    assertEquals(acc, 1 + 3)
  }

  test("InlinedIterable should work in a for loop with yield") {
    val items = InlinedIterable(Seq(1, 2, 3))

    val items2 = for item <- items yield {
      item + 3
    }

    assertEquals(items2.toSeq, Seq(4, 5, 6))
  }

  test("InlinedIterable should work in a for loop with yield and filter") {
    val items = InlinedIterable(Seq(1, 2, 3))

    val items2 = for item <- items if item != 2 yield {
      item + 3
    }

    assertEquals(items2.toSeq, Seq(4, 6))
  }
}
