/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

/**
 * Generic exception for unexpected error cases.
 */
case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None)
    extends PlayException(
      "Unexpected exception",
      message.getOrElse {
        unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
      },
      unexpected.orNull
    )
