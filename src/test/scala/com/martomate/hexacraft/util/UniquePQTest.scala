package com.martomate.hexacraft.util

import munit.FunSuite

class UniquePQTest extends FunSuite {
  private val defOrder = Ordering.Double.TotalOrdering

  test("an empty queue should have size 0 from the beginning") {
    assertEquals(new UniquePQ[String](_ => 1, defOrder).size, 0)
  }
  test("an empty queue should be empty") {
    assert(new UniquePQ[String](_ => 1, defOrder).isEmpty)
  }
  test("an empty queue should have size 1 after enqueue") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    assertEquals(q.size, 1)
  }
  test("an empty queue should fail to dequeue") {
    intercept[NoSuchElementException](new UniquePQ[String](_ => 1, defOrder).dequeue())
  }

  test("a non-empty queue should not be empty") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    assert(!q.isEmpty)
  }
  test("a non-empty queue should have the right size from the beginning") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("c")
    assertEquals(q.size, 3)
  }
  test("a non-empty queue should increase it's size after enqueueing a new element") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("a")
    q.dequeue()
    q.enqueue("c")
    q.dequeue()
    q.enqueue("c")
    val s = q.size
    q.enqueue("d")
    assertEquals(q.size, s + 1)
  }
  test("a non-empty queue should not increase it's size after enqueueing an existing element") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    val s = q.size
    q.enqueue("a")
    assertEquals(q.size, s)
  }
  test("a non-empty queue should decrease it's size after dequeue") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("a")
    q.enqueue("c")
    val s = q.size
    q.dequeue()
    assertEquals(q.size, s - 1)
  }
  test("a non-empty queue should allow items to be re-added after removal") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    assertEquals(q.size, 1)
    q.dequeue()
    assertEquals(q.size, 0)
    q.enqueue("a")
    assertEquals(q.size, 1)
  }
  test("dequeue should return items with highest priority first") {
    val q = new UniquePQ[String](s => s.length, defOrder)
    q.enqueue("aa")
    q.enqueue("a")
    q.enqueue("aaa")
    assertEquals(q.dequeue(), "aaa")
    assertEquals(q.dequeue(), "aa")
    assertEquals(q.dequeue(), "a")
  }
  test("clear should remove all items, resetting the queue") {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.enqueue("b")
    q.clear()
    assertEquals(q.size, 0)
    assert(q.isEmpty)
    q.enqueue("a")
    assertEquals(q.size, 1)
  }
  test("reprioritizeAndFilter should reorder items") {
    var reference = 1
    def f(s: String): Double = math.abs(s.length - reference)
    val q = new UniquePQ[String](f, defOrder)
    q.enqueue("a") // a   -> |1-1| = 0
    q.enqueue("aa") // aa  -> |2-1| = 1
    q.enqueue("aaa") // aaa -> |3-1| = 2

    reference = 3
    q.reprioritizeAndFilter(_ => true)

    assertEquals(q.dequeue(), "a") //   a   -> |1-3| = 2
    assertEquals(q.dequeue(), "aa") //  aa  -> |2-3| = 1
    assertEquals(q.dequeue(), "aaa") // aaa -> |3-3| = 0
  }
  test("reprioritizeAndFilter should filter out items") {
    val q = new UniquePQ[String](s => s.length, defOrder)
    q.enqueue("a")
    q.enqueue("aa")
    q.enqueue("aaa")

    q.reprioritizeAndFilter { case (_, s) => !s.equals("aa") }

    assertEquals(q.dequeue(), "aaa")
    assertEquals(q.dequeue(), "a")
  }
}
