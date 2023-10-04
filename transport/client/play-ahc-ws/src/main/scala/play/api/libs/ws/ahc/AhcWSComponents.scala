/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws.ahc

import scala.concurrent.ExecutionContext

import org.apache.pekko.stream.Materializer
import play.api._
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.WSClient
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient

/**
 * AsyncHttpClient WS API implementation components.
 */
trait AhcWSComponents {
  def environment: Environment

  def configuration: Configuration

  def applicationLifecycle: ApplicationLifecycle

  def materializer: Materializer

  def executionContext: ExecutionContext

  lazy val wsClient: WSClient = {
    implicit val mat: Materializer = materializer
    new AhcWSClientProvider(standaloneWSClient.asInstanceOf[StandaloneAhcWSClient]).get
  }

  lazy val standaloneWSClient: StandaloneWSClient = {
    implicit val mat: Materializer = materializer
    new StandaloneAhcWSClient(asyncHttpClient)
  }

  lazy val asyncHttpClient: AsyncHttpClient = {
    implicit val ec: ExecutionContext = executionContext
    new AsyncHttpClientProvider(environment, configuration, applicationLifecycle).get
  }

}
