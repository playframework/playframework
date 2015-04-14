/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

/**
 * Indicates an issue with starting a server, e.g. a problem reading its
 * configuration.
 */
final case class ServerStartException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)
