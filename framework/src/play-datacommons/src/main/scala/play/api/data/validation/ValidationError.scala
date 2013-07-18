package play.api.data.validation

/**
 * A validation error.
 *
 * @param message the error message
 * @param args the error message arguments
 */
case class ValidationError(message: String, args: Any*)

