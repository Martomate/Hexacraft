package hexacraft.util

import munit.FunSuite

class UniquePQTest extends FunSuite {
  private val defOrder = Ordering.Double.TotalOrdering

  test("an empty queue should have size 0 from the beginning") {
    assertEquals(new UniqueLongPQ(_ => 1, defOrder).size, 0)
  }
  test("an empty queue should be empty") {
    assert(new UniqueLongPQ(_ => 1, defOrder).isEmpty)
  }
  test("an empty queue should have size 1 after enqueue") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    assertEquals(q.size, 1)
  }
  test("an empty queue should fail to dequeue") {
    intercept[NoSuchElementException](new UniqueLongPQ(_ => 1, defOrder).dequeue())
  }

  test("a non-empty queue should not be empty") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    assert(!q.isEmpty)
  }
  test("a non-empty queue should have the right size from the beginning") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    q.enqueue(14)
    q.enqueue(15)
    assertEquals(q.size, 3)
  }
  test("a non-empty queue should increase it's size after enqueueing a new element") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    q.enqueue(14)
    q.enqueue(13)
    q.dequeue()
    q.enqueue(15)
    q.dequeue()
    q.enqueue(15)
    val s = q.size
    q.enqueue(16)
    assertEquals(q.size, s + 1)
  }
  test("a non-empty queue should not increase it's size after enqueueing an existing element") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    val s = q.size
    q.enqueue(13)
    assertEquals(q.size, s)
  }
  test("a non-empty queue should decrease it's size after dequeue") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    q.enqueue(14)
    q.enqueue(13)
    q.enqueue(15)
    val s = q.size
    q.dequeue()
    assertEquals(q.size, s - 1)
  }
  test("a non-empty queue should allow items to be re-added after removal") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    assertEquals(q.size, 1)
    q.dequeue()
    assertEquals(q.size, 0)
    q.enqueue(13)
    assertEquals(q.size, 1)
  }
  test("dequeue should return items with highest priority first") {
    val q = new UniqueLongPQ(s => -math.abs(s - 11).toDouble, defOrder)
    q.enqueue(10)
    q.enqueue(13)
    q.enqueue(11)
    assertEquals(q.dequeue(), 11L)
    assertEquals(q.dequeue(), 10L)
    assertEquals(q.dequeue(), 13L)
  }
  test("clear should remove all items, resetting the queue") {
    val q = new UniqueLongPQ(_ => 1, defOrder)
    q.enqueue(13)
    q.enqueue(14)
    q.clear()
    assertEquals(q.size, 0)
    assert(q.isEmpty)
    q.enqueue(13)
    assertEquals(q.size, 1)
  }
  test("reprioritizeAndFilter should reorder items") {
    var reference = 13
    def f(s: Long): Double = math.abs(s - reference).toDouble
    val q = new UniqueLongPQ(f, defOrder)
    q.enqueue(13)
    q.enqueue(10)
    q.enqueue(11)

    reference = 11
    q.reprioritizeAndFilter(_ => true)

    assertEquals(q.dequeue(), 13L)
    assertEquals(q.dequeue(), 10L)
    assertEquals(q.dequeue(), 11L)
  }
  test("reprioritizeAndFilter should filter out items") {
    val q = new UniqueLongPQ(s => -s.toDouble, defOrder)
    q.enqueue(13)
    q.enqueue(10)
    q.enqueue(11)

    q.reprioritizeAndFilter { case (_, s) => !s.equals(10L) }

    assertEquals(q.dequeue(), 11L)
    assertEquals(q.dequeue(), 13L)
  }
}
