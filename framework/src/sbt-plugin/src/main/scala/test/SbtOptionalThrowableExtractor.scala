package test

import sbt.testing.OptionalThrowable

/**
 * Extractor object for SBTs OptionalThrowable
 */
object SbtOptionalThrowable {
  def unapply(ot: OptionalThrowable): Option[Throwable] = {
    ot match {
      case throwable if throwable.isDefined => Some(throwable.get)
      case _ => None
    }
  }
}
