/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.data.validation

/**
 * A validation error.
 *
 * @param message the error message
 * @param args the error message arguments
 */
case class ValidationError(message: String, args: Any*)

