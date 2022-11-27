package com.martomate.hexacraft.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UniquePQTest extends AnyFlatSpec with Matchers {
  private val defOrder = Ordering.Double.TotalOrdering

  "an empty queue" should "have size 0 from the beginning" in {
    new UniquePQ[String](_ => 1, defOrder).size shouldBe 0
  }
  it should "be empty" in {
    new UniquePQ[String](_ => 1, defOrder).isEmpty shouldBe true
  }
  it should "have size 1 after enqueue" in {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.size shouldBe 1
  }
  it should "fail to dequeue" in {
    an[NoSuchElementException] should be thrownBy new UniquePQ[String](_ => 1, defOrder).dequeue()
  }

  "a non-empty queue" should "not be empty" in {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.isEmpty shouldBe false
  }
  it should "have the right size from the beginning" in {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("c")
    q.size shouldBe 3
  }
  it should "increase it's size after enqueueing a new element" in {
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
    q.size shouldBe s + 1
  }
  it should "not increase it's size after enqueueing an existing element" in {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    val s = q.size
    q.enqueue("a")
    q.size shouldBe s
  }
  it should "decrease it's size after dequeue" in {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.enqueue("b")
    q.enqueue("a")
    q.enqueue("c")
    val s = q.size
    q.dequeue()
    q.size shouldBe s - 1
  }
  it should "allow items to be re-added after removal" in {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.size shouldBe 1
    q.dequeue()
    q.size shouldBe 0
    q.enqueue("a")
    q.size shouldBe 1
  }
  "dequeue" should "return items with highest priority first" in {
    val q = new UniquePQ[String](s => s.length, defOrder)
    q.enqueue("aa")
    q.enqueue("a")
    q.enqueue("aaa")
    q.dequeue() shouldBe "aaa"
    q.dequeue() shouldBe "aa"
    q.dequeue() shouldBe "a"
  }
  "clear" should "remove all items, resetting the queue" in {
    val q = new UniquePQ[String](_ => 1, defOrder)
    q.enqueue("a")
    q.enqueue("b")
    q.clear()
    q.size shouldBe 0
    q.isEmpty shouldBe true
    q.enqueue("a")
    q.size shouldBe 1
  }
  "reprioritizeAndFilter" should "reorder items" in {
    var reference = 1
    def f(s: String): Double = math.abs(s.length - reference)
    val q = new UniquePQ[String](f, defOrder)
    q.enqueue("a") // a   -> |1-1| = 0
    q.enqueue("aa") // aa  -> |2-1| = 1
    q.enqueue("aaa") // aaa -> |3-1| = 2

    reference = 3
    q.reprioritizeAndFilter(_ => true)

    q.dequeue() shouldBe "a" // a   -> |1-3| = 2
    q.dequeue() shouldBe "aa" // aa  -> |2-3| = 1
    q.dequeue() shouldBe "aaa" // aaa -> |3-3| = 0
  }
  it should "filter out items" in {
    val q = new UniquePQ[String](s => s.length, defOrder)
    q.enqueue("a")
    q.enqueue("aa")
    q.enqueue("aaa")

    q.reprioritizeAndFilter { case (_, s) => !s.equals("aa") }

    q.dequeue() shouldBe "aaa"
    q.dequeue() shouldBe "a"
  }
}
