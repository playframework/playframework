package play.api.libs.json

/**
 * A JSON validation error representation.
 */
case class JsonValidationError(messages: Seq[String], args: Any*) {
  lazy val message = messages.last
}

object JsonValidationError {
  def apply(message: String, args: Any*) = new JsonValidationError(Seq(message), args: _*)
}
