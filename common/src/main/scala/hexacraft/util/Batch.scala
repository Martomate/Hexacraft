package hexacraft.util

import hexacraft.util.Result.Ok

class Batch[A, E](state: Result[Seq[A], E]) {
  def flatMap[B](f: A => Result[B, E]): Batch[B, E] = {
    val r = for {
      values <- state
      result <- Result.all(values)(f)
    } yield result
    Batch(r)
  }

  def map[B](f: A => B): Batch[B, E] = {
    Batch(state.map(_.map(f)))
  }

  /** @return the final result of sending all values through the processing steps */
  def toResult: Result[Seq[A], E] = state
}

/** Batch allows for processing several items in several processing steps.
  * It aborts the entire procedure if an error occurs.
  */
object Batch {

  /** Produces a Batch pipeline with the given input values
    *
    * @tparam A the type of values to process
    * @tparam E the type of error that can occur
    */
  def of[A, E](inputs: Seq[A]): Batch[A, E] = Batch(Ok(inputs))
}
