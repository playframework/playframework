/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

//#custom-akka-http-server
//###replace: package server
package detailedtopics.configuration.customakkaserver

import java.util.Random
import play.core.server.{AkkaHttpServer, AkkaHttpServerProvider, ServerProvider}
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.settings.{ParserSettings, ServerSettings}

/** A custom Akka HTTP server with advanced configuration. */
class CustomAkkaHttpServer(context: AkkaHttpServer.Context) extends AkkaHttpServer(context) {
  override protected def createParserSettings(): ParserSettings = {
    val defaultSettings: ParserSettings =
      super.createParserSettings()
    defaultSettings.withCustomMethods(HttpMethod.custom("TICKLE"))
  }
  override protected def createServerSettings(port: Int, connectionContext: ConnectionContext, secure: Boolean): ServerSettings = {
    val defaultSettings: ServerSettings =
      super.createServerSettings(port, connectionContext, secure)
    defaultSettings.withWebsocketRandomFactory(() => new Random())
  }
}

/** A factory that instantiates a CustomAkkaHttpServer. */
class CustomAkkaHttpServerProvider extends ServerProvider {
  def createServer(context: ServerProvider.Context) = {
    val serverContext = AkkaHttpServer.Context.fromServerProviderContext(context)
    new CustomAkkaHttpServer(serverContext)
  }
}
//#custom-akka-http-server
