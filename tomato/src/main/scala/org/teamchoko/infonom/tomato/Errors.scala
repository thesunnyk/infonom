package org.teamchoko.infonom.tomato

import scalaz.{-\/, \/-, \/}
import scalaz.\/.fromTryCatchNonFatal
import scalaz.\/.right

object Errors {
  type StringError[T] = String \/ T
  
  def extractErrors[T](item: => T): StringError[T] = fromTryCatchNonFatal(item).leftMap(_.getMessage())

  def success[T](t: T) = \/-(t)

  def failure(t: String) = -\/(t)
  
  def checkTrue(succ: Boolean, msg: String): StringError[Unit] = if (succ) success() else failure(msg)

}
