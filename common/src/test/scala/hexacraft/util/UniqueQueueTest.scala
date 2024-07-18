package hexacraft.util

import munit.FunSuite

class UniqueQueueTest extends FunSuite {
  test("an empty queue should have size 0 from the beginning") {
    assertEquals((new UniqueLongQueue).size, 0)
  }

  test("an empty queue should be empty") {
    assert((new UniqueLongQueue).isEmpty)
  }

  test("an empty queue should have size 1 after enqueue") {
    val q = new UniqueLongQueue
    q.enqueue(13)
    assertEquals(q.size, 1)
  }

  test("an empty queue should fail to dequeue") {
    intercept[NoSuchElementException]((new UniqueLongQueue).dequeue())
  }

  test("a non-empty queue should not be empty") {
    val q = new UniqueLongQueue
    q.enqueue(13)
    assert(!q.isEmpty)
  }

  test("a non-empty queue should have the right size from the beginning") {
    val q = new UniqueLongQueue
    q.enqueue(13)
    q.enqueue(14)
    q.enqueue(15)
    assertEquals(q.size, 3)
  }

  test("a non-empty queue should not increase it's size after enqueueing an existing element") {
    val q = new UniqueLongQueue
    q.enqueue(13)
    val s = q.size
    q.enqueue(13)
    assertEquals(q.size, s)
  }

  test("a non-empty queue should decrease it's size after dequeue") {
    val q = new UniqueLongQueue
    q.enqueue(13)
    q.enqueue(14)
    q.enqueue(13)
    q.enqueue(15)
    val s = q.size
    q.dequeue()
    assertEquals(q.size, s - 1)
  }

  test("a non-empty queue should allow items to be re-added after removal") {
    val q = new UniqueLongQueue
    q.enqueue(13)
    assertEquals(q.size, 1)
    q.dequeue()
    assertEquals(q.size, 0)
    q.enqueue(13)
    assertEquals(q.size, 1)
  }

  test("dequeue should return the first element added") {
    val q = new UniqueLongQueue
    q.enqueue(10)
    q.enqueue(13)
    q.enqueue(11)
    assertEquals(q.dequeue(), 10L)
    assertEquals(q.dequeue(), 13L)
    assertEquals(q.dequeue(), 11L)
  }

  test("clear should remove all items, resetting the queue") {
    val q = new UniqueLongQueue
    q.enqueue(13)
    q.enqueue(14)
    q.clear()
    assertEquals(q.size, 0)
    assert(q.isEmpty)
    q.enqueue(13)
    assertEquals(q.size, 1)
  }
}
