/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import play.api.mvc._

/**
 * This exception occurs when the Play server experiences an error
 * while processing a Result. For example, this exception might occur
 * when attempting to convert the Play result to the backend server's
 * internal response format.
 *
 * @param message The reason for the exception.
 * @param result The invalid result.
 * @param cause The original cause (may be null).
 */
class ServerResultException(message: String, val result: Result, cause: Throwable) extends Exception(message, cause)
