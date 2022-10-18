/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

/**
 * Indicates an issue with starting a server, e.g. a problem reading its
 * configuration.
 */
final case class ServerStartException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)
