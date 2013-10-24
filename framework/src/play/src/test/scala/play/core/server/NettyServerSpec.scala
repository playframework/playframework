/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import scala.util.{ Try, Failure }

import org.specs2.mutable.Specification
import play.core.ApplicationProvider
import java.io.File
import scala.util.Random
import play.api.Application
import play.api.libs.ws.WS
import scala.concurrent.Await
import scala.concurrent.duration._


object NettyServerSpec extends Specification {

  class Fake extends ApplicationProvider {
    def path: File = new File(".")
    def get: Try[Application] = Failure(new RuntimeException)
  }


  "NettyServer" should {
    "fail when no https.port and http.port is missing" in {
      new NettyServer(
        new Fake,
        None,
        None
      ) must throwAn[IllegalArgumentException]
    }
  }


}
