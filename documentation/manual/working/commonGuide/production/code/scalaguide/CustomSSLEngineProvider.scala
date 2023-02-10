/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide

// #scalaexample
import javax.inject.Inject
import javax.net.ssl._

import play.core.ApplicationProvider
import play.server.api._

class CustomSSLEngineProvider @Inject() (appProvider: ApplicationProvider) extends SSLEngineProvider {
  override def createSSLEngine(): SSLEngine = {
    // change it to your custom implementation
    sslContext().createSSLEngine
  }

  override def sslContext(): SSLContext = {
    // change it to your custom implementation
    SSLContext.getDefault
  }
}
// #scalaexample
