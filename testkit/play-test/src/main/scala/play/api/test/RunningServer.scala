/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import org.apache.pekko.annotation.ApiMayChange
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
