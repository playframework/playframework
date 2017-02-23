/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.data.validation

/**
 * A validation error.
 *
 * @param messages the error message, if more then one message is passed it will use the last one
 * @param args the error message arguments
 */
case class ValidationError(messages: Seq[String], args: Any*) {

  lazy val message = messages.last

}

object ValidationError {

  def apply(message: String, args: Any*) = new ValidationError(Seq(message), args: _*)

}
