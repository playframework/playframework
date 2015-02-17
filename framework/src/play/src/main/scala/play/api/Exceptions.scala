/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

/**
 * Generic exception for unexpected error cases.
 */
case class UnexpectedException(message: Option[String] = None, unexpected: Option[Throwable] = None) extends PlayException(
  "Unexpected exception",
  message.getOrElse {
    unexpected.map(t => "%s: %s".format(t.getClass.getSimpleName, t.getMessage)).getOrElse("")
  },
  unexpected.orNull
)
