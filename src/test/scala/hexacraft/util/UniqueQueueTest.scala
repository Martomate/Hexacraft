package hexacraft.util

import munit.FunSuite

class UniqueQueueTest extends FunSuite {
  test("an empty queue should have size 0 from the beginning") {
    assertEquals(new UniqueQueue[String].size, 0)
  }

  test("an empty queue should be empty") {
    assert(new UniqueQueue[String].isEmpty)
  }

  test("an empty queue should have size 1 after enqueue") {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    assertEquals(q.size, 1)
  }

  test("an empty queue should fail to dequeue") {
    intercept[NoSuchElementException](new UniqueQueue[String].dequeue())
  }

  test("a non-empty queue should not be empty") {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    assert(!q.isEmpty)
  }

  test("a non-empty queue should have the right size from the beginning") {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("c")
    assertEquals(q.size, 3)
  }

  test("a non-empty queue should not increase it's size after enqueueing an existing element") {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    val s = q.size
    q.enqueue("a")
    assertEquals(q.size, s)
  }

  test("a non-empty queue should decrease it's size after dequeue") {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("a")
    q.enqueue("c")
    val s = q.size
    q.dequeue()
    assertEquals(q.size, s - 1)
  }

  test("a non-empty queue should allow items to be re-added after removal") {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    assertEquals(q.size, 1)
    q.dequeue()
    assertEquals(q.size, 0)
    q.enqueue("a")
    assertEquals(q.size, 1)
  }

  test("dequeue should return the first element added") {
    val q = new UniqueQueue[String]
    q.enqueue("aa")
    q.enqueue("a")
    q.enqueue("aaa")
    assertEquals(q.dequeue(), "aa")
    assertEquals(q.dequeue(), "a")
    assertEquals(q.dequeue(), "aaa")
  }

  test("clear should remove all items, resetting the queue") {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.enqueue("b")
    q.clear()
    assertEquals(q.size, 0)
    assert(q.isEmpty)
    q.enqueue("a")
    assertEquals(q.size, 1)
  }
}
