package hexacraft.util

import hexacraft.util.Result.{Err, Ok}

import munit.FunSuite

class BatchTest extends FunSuite {
  test("process one item with direct mapping") {
    val r = for item <- Batch.of(Seq(5)) yield item + 2
    assertEquals(r.toResult, Ok(Seq(7)))
  }

  test("process multiple item with direct mapping") {
    val r = for item <- Batch.of(Seq(5, 6, 7, 5)) yield item + 2
    assertEquals(r.toResult, Ok(Seq(7, 8, 9, 7)))
  }

  test("process one item with one step") {
    val r = for {
      a <- Batch.of(Seq(5))
      b <- Ok(a + 3)
    } yield b + 2
    assertEquals(r.toResult, Ok(Seq(10)))
  }

  test("process multiple items with one step") {
    val r = for {
      a <- Batch.of(Seq(5, 6, 7, 5))
      b <- Ok(a + 3)
    } yield b + 2
    assertEquals(r.toResult, Ok(Seq(10, 11, 12, 10)))
  }

  test("process one item with multiple steps") {
    val r = for {
      a <- Batch.of(Seq(5))
      b <- Ok(a + 4)
      c <- Ok(b - 1)
    } yield c + 2
    assertEquals(r.toResult, Ok(Seq(10)))
  }

  test("process multiple items with multiple steps") {
    val r = for {
      a <- Batch.of(Seq(5, 6, 7, 5))
      b <- Ok(a + 4)
      c <- Ok(b - 1)
    } yield c + 2
    assertEquals(r.toResult, Ok(Seq(10, 11, 12, 10)))
  }

  test("process one item with failing step") {
    val r = for {
      a <- Batch.of(Seq(5))
      b <- if a == 100 then Ok(42) else Err("noo!!")
      c <- Ok(b - 1)
    } yield c + 2
    assertEquals(r.toResult, Err("noo!!"))
  }

  test("process multiple items with failing step") {
    val r = for {
      a <- Batch.of(Seq(100, 5, 100))
      b <- if a == 100 then Ok(42) else Err("noo!!")
      c <- Ok(b - 1)
    } yield c + 2
    assertEquals(r.toResult, Err("noo!!"))
  }

  // TODO: in the future it might make sense to relax this requirement
  test("a failing step aborts all future steps") {
    var nextStepRan = false
    val r = for {
      a <- Batch.of(Seq(5, 100))
      b <- if a == 100 then Ok(42) else Err("noo!!")
      c <- {
        nextStepRan = true
        Ok(b - 1)
      }
    } yield c + 2
    assert(!nextStepRan)
  }
}
