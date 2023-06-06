package com.martomate.hexacraft.util

import scala.collection.mutable.ArrayBuffer

sealed trait Result[+T, +E]:
  def map[T2](f: T => T2): Result[T2, E]

  def mapErr[E2](f: E => E2): Result[T, E2]

  def andThen[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2]

  def flatMap[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2] = this.andThen(f)

object Result:
  case class Ok[+T, +E](value: T) extends Result[T, E]:
    def map[T2](f: T => T2): Result[T2, E] = Ok(f(value))

    def mapErr[E2](f: E => E2): Result[T, E2] = Ok(value)

    def andThen[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2] = f(value)

  case class Err[+T, +E](error: E) extends Result[T, E]:
    def map[T2](f: T => T2): Result[T2, E] = Err(error)

    def mapErr[E2](f: E => E2): Result[T, E2] = Err(f(error))

    def andThen[T2, E2 >: E](f: T => Result[T2, E2]): Result[T2, E2] = Err(error)

  def fromOption[T, E](opt: Option[T], errorIfNone: E): Result[T, E] = opt match
    case Some(value) => Ok(value)
    case None        => Err(errorIfNone)

  def fromOption[T](opt: Option[T]): Result[T, Unit] = opt match
    case Some(value) => Ok(value)
    case None        => Err(())

  def split[T, E](results: Seq[Result[T, E]]): (Seq[T], Seq[E]) =
    val oks = ArrayBuffer.empty[T]
    val errs = ArrayBuffer.empty[E]

    for r <- results do
      r match
        case Ok(v)  => oks += v
        case Err(e) => errs += e

    (oks.toSeq, errs.toSeq)
