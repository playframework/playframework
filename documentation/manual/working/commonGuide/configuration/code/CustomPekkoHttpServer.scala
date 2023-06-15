/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

//#custom-pekko-http-server
// ###replace: package server
package detailedtopics.configuration.customakkaserver

import java.util.Random

import org.apache.pekko.http.scaladsl.model.HttpMethod
import org.apache.pekko.http.scaladsl.settings.ParserSettings
import org.apache.pekko.http.scaladsl.settings.ServerSettings
import org.apache.pekko.http.scaladsl.ConnectionContext
import play.core.server.AkkaHttpServer
import play.core.server.AkkaHttpServerProvider
import play.core.server.ServerProvider

/** A custom Akka HTTP server with advanced configuration. */
class CustomAkkaHttpServer(context: AkkaHttpServer.Context) extends AkkaHttpServer(context) {
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

/** A factory that instantiates a CustomAkkaHttpServer. */
class CustomAkkaHttpServerProvider extends ServerProvider {
  def createServer(context: ServerProvider.Context): CustomAkkaHttpServer = {
    val serverContext = AkkaHttpServer.Context.fromServerProviderContext(context)
    new CustomAkkaHttpServer(serverContext)
  }
}
//#custom-pekko-http-server
