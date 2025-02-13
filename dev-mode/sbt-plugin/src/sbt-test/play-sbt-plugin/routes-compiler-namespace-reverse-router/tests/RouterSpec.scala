/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test

import play.api.test._

object RouterSpec extends PlaySpecification {

  "document the router" in new WithApplication() {
    override def running() = {
      val someRoute = implicitApp.injector
        .instanceOf[play.api.routing.Router]
        .documentation
        .find(r => r._1 == "GET" && r._2.startsWith("/public/"))
      someRoute must beSome[(String, String, String)]
      val route = someRoute.get
      route._2 must_== "/public/$file<.+>"
      route._3 must startWith(
        """_root_.controllers.Assets.versioned(file:String)"""
      )
    }
  }

  "The assets reverse route support" should {
    "fingerprint assets" in new WithApplication() {
      override def running() = {
        app.injector.instanceOf[controllers.AssetsFinder].path("css/main.css") must_== "/public/css/abcd1234-main.css"
      }
    }
    "selected the minified version" in new WithApplication() {
      override def running() = {
        app.injector.instanceOf[controllers.AssetsFinder].path("css/minmain.css") must_== "/public/css/abcd1234-minmain-min.css"
      }
    }
    "work for non fingerprinted assets" in new WithApplication() {
      override def running() = {
        app.injector.instanceOf[controllers.AssetsFinder]
          .path("css/nonfingerprinted.css") must_== "/public/css/nonfingerprinted.css"
      }
    }
    "selected the minified non fingerprinted version" in new WithApplication() {
      override def running() = {
        app.injector.instanceOf[controllers.AssetsFinder]
          .path("css/nonfingerprinted-minmain.css") must_== "/public/css/nonfingerprinted-minmain-min.css"
      }
    }
  }
}
