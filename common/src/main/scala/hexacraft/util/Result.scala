package hexacraft.util

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

sealed trait Result[+T, +E] {
  def isOk: Boolean

  def isErr: Boolean

  def unwrap(): T

  def unwrapErr(): E

  def unwrapWith(f: E => Throwable): T

  def map[T2](f: T => T2): Result[T2, E]

  def mapErr[E2](f: E => E2): Result[T, E2]

  def andThen[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2]

  def flatMap[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2] = this.andThen(f)
}

object Result {
  case class Ok[+T, +E](value: T) extends Result[T, E] {
    def isOk: Boolean = true

    def isErr: Boolean = false

    def unwrap(): T = value

    def unwrapWith(f: E => Throwable): T = value

    def unwrapErr(): E = throw new NoSuchElementException("Result was not Err")

    def map[T2](f: T => T2): Result[T2, E] = Ok(f(value))

    def mapErr[E2](f: E => E2): Result[T, E2] = Ok(value)

    def andThen[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2] = f(value)
  }

  case class Err[+T, +E](error: E) extends Result[T, E] {
    def isOk: Boolean = false

    def isErr: Boolean = true

    def unwrap(): T = throw new RuntimeException(error.toString)

    def unwrapWith(f: E => Throwable): T = throw f(error)

    def unwrapErr(): E = error

    def map[T2](f: T => T2): Result[T2, E] = Err(error)

    def mapErr[E2](f: E => E2): Result[T, E2] = Err(f(error))

    def andThen[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2] = Err(error)
  }

  def fromOption[T, E](opt: Option[T], errorIfNone: E): Result[T, E] = opt match {
    case Some(value) => Ok(value)
    case None        => Err(errorIfNone)
  }

  def fromOption[T](opt: Option[T]): Result[T, Unit] = opt match {
    case Some(value) => Ok(value)
    case None        => Err(())
  }

  def fromTry[T](t: Try[T]): Result[T, Throwable] = t match {
    case Success(value) => Ok(value)
    case Failure(e)     => Err(e)
  }

  def attempt[T](op: => T): Result[T, Throwable] = {
    try {
      val value = op
      Ok(value)
    } catch {
      case t: Throwable => Err(t)
    }
  }

  def split[T, E](results: Iterable[Result[T, E]]): (Seq[T], Seq[E]) = {
    val oks = ArrayBuffer.empty[T]
    val errs = ArrayBuffer.empty[E]

    for r <- results do {
      r match {
        case Ok(v)  => oks += v
        case Err(e) => errs += e
      }
    }

    (oks.toSeq, errs.toSeq)
  }

  def all[A, B, E](s: Iterable[A])(f: A => Result[B, E]): Result[Seq[B], E] = {
    val values = new mutable.ArrayBuffer[B](s.size)

    val it = s.iterator
    while it.hasNext do {
      val a = it.next
      f(a) match {
        case Ok(v)  => values += v
        case Err(e) => return Err(e)
      }
    }

    Ok(values.toSeq)
  }

  extension [A, E <: Throwable](r: Result[A, E]) {
    def toTry: Try[A] = {
      r match {
        case Ok(value)  => Success(value)
        case Err(error) => Failure(error)
      }
    }
  }
}
