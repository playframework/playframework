/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import akka.annotation.ApiMayChange

/**
 * Used as request attribute which gets attached to the request that gets passed to an error
 * handler. Contains additional information useful for handling an error.
 */
@ApiMayChange
case class HttpErrorInfo(
    /**
     * The origin of where the error handler was initially called.<br>
     * Play currently adds following values:
     *
     * <ul>
     * <li>{@code server-backend} - The error handler was called in either the Netty or Akka-HTTP
     * server backend.
     * <li>{@code csrf-filter} - The error handler was called in CSRF filter code.
     * <li>{@code csp-filter} - The error handler was called in CSP filter code.
     * <li>{@code allowed-hosts-filter} - The error handler was called in Allowed hosts filter code.
     * </ul>
     *
     * Third party modules may add their own origins.
     */
    origin: String
) extends play.http.HttpErrorInfo
