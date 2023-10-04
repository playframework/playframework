/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

//#custom-pekko-http-server
// ###replace: package server
package detailedtopics.configuration.custompekkoserver

import java.util.Random

import org.apache.pekko.http.scaladsl.model.HttpMethod
import org.apache.pekko.http.scaladsl.settings.ParserSettings
import org.apache.pekko.http.scaladsl.settings.ServerSettings
import org.apache.pekko.http.scaladsl.ConnectionContext
import play.core.server.PekkoHttpServer
import play.core.server.PekkoHttpServerProvider
import play.core.server.ServerProvider

/** A custom Pekko HTTP server with advanced configuration. */
class CustomPekkoHttpServer(context: PekkoHttpServer.Context) extends PekkoHttpServer(context) {
  protected override def createParserSettings(): ParserSettings = {
    val defaultSettings: ParserSettings =
      super.createParserSettings()
    defaultSettings.withCustomMethods(HttpMethod.custom("TICKLE"))
  }
  protected override def createServerSettings(
      port: Int,
      connectionContext: ConnectionContext,
      secure: Boolean
  ): ServerSettings = {
    val defaultSettings: ServerSettings =
      super.createServerSettings(port, connectionContext, secure)
    defaultSettings.withWebsocketRandomFactory(() => new Random())
  }
}

/** A factory that instantiates a CustomPekkoHttpServer. */
class CustomPekkoHttpServerProvider extends ServerProvider {
  def createServer(context: ServerProvider.Context): CustomPekkoHttpServer = {
    val serverContext = PekkoHttpServer.Context.fromServerProviderContext(context)
    new CustomPekkoHttpServer(serverContext)
  }
}
//#custom-pekko-http-server
