package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UniqueQueueTest extends AnyFlatSpec with Matchers {
  "an empty queue" should "have size 0 from the beginning" in {
    new UniqueQueue[String].size shouldBe 0
  }

  it should "be empty" in {
    new UniqueQueue[String].isEmpty shouldBe true
  }

  it should "have size 1 after enqueue" in {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.size shouldBe 1
  }

  it should "fail to dequeue" in {
    an[NoSuchElementException] should be thrownBy new UniqueQueue[String].dequeue()
  }

  "a non-empty queue" should "not be empty" in {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.isEmpty shouldBe false
  }

  it should "have the right size from the beginning" in {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("c")
    q.size shouldBe 3
  }

  it should "not increase it's size after enqueueing an existing element" in {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    val s = q.size
    q.enqueue("a")
    q.size shouldBe s
  }

  it should "decrease it's size after dequeue" in {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("a")
    q.enqueue("c")
    val s = q.size
    q.dequeue()
    q.size shouldBe s - 1
  }

  it should "allow items to be re-added after removal" in {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.size shouldBe 1
    q.dequeue()
    q.size shouldBe 0
    q.enqueue("a")
    q.size shouldBe 1
  }

  "dequeue" should "return the first element added" in {
    val q = new UniqueQueue[String]
    q.enqueue("aa")
    q.enqueue("a")
    q.enqueue("aaa")
    q.dequeue() shouldBe "aa"
    q.dequeue() shouldBe "a"
    q.dequeue() shouldBe "aaa"
  }

  "clear" should "remove all items, resetting the queue" in {
    val q = new UniqueQueue[String]
    q.enqueue("a")
    q.enqueue("b")
    q.clear()
    q.size shouldBe 0
    q.isEmpty shouldBe true
    q.enqueue("a")
    q.size shouldBe 1
  }
}
