/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data.validation

/**
 * A validation error.
 *
 * @param message the error message
 * @param args the error message arguments
 */
case class ValidationError private (messages: Seq[String], args: Seq[Any] = Seq.empty, fieldName: Option[String] = None) {
  lazy val message = messages.last

  def forField(name: String) = this.copy(fieldName = Some(name))
}

object ValidationError {

  def apply(message: String, args: Any*) = new ValidationError(Seq(message), args, None)

  def apply(messages: Seq[String], args: Any*) = new ValidationError(messages, args, None)

}