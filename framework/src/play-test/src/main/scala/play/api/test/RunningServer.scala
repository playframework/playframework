/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import akka.annotation.ApiMayChange

import play.api.Application
import play.core.server.ServerEndpoints

/**
 * Contains information about a running TestServer. This object can be
 * used by tests to find out about the running server, e.g. port information.
 *
 * We use a separate class to avoid including mutable state, such as methods
 * for closing the server.
 */
@ApiMayChange final case class RunningServer(
    app: Application,
    endpoints: ServerEndpoints,
    stopServer: AutoCloseable
)
