/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.server.api

import javax.net.ssl.SSLEngine

/**
 * To configure the SSLEngine used by Play as a server, extend this class. See more details in [[play.server.SSLEngineProvider]].
 */
trait SSLEngineProvider extends play.server.SSLEngineProvider {

  /**
   * @return the SSL engine to be used for HTTPS connection.
   */
  override def createSSLEngine: SSLEngine
}
