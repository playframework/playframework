/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server

import scala.util.{ Try, Failure }

import org.specs2.mutable.{Around, Specification}
import play.core.ApplicationProvider
import java.io.File
import scala.util.Random
import play.api.Application
import scala.concurrent.Await
import scala.concurrent.duration._
import org.specs2.execute.{Result, AsResult}


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

    "use the backlog system property" in {
      System.setProperty("play.netty.backlog", "10")
      val server = new NettyServer(
        new Fake,
        Some(9191),
        None
      )
      try {
        val Some((bootstrap, _)) = server.HTTP
        bootstrap.getOptions.get("backlog") must_== 10
      } finally {
        server.stop()
        System.clearProperty("play.netty.backlog")
      }
    }
  }


}
