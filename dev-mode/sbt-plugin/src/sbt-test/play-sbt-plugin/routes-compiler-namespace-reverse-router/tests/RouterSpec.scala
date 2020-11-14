/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package test

import play.api.test._

object RouterSpec extends PlaySpecification {

  "document the router" in new WithApplication() {
    val someRoute = implicitApp.injector
      .instanceOf[play.api.routing.Router]
      .documentation
      .find(r => r._1 == "GET" && r._2.startsWith("/public/"))
    someRoute must beSome[(String, String, String)]
    val route = someRoute.get
    route._2 must_== "/public/$file<.+>"
    route._3 must startWith("""_root_.controllers.Assets.versioned(path:String = "/public", file:_root_.controllers.Assets.Asset)""")
  }

  "The assets reverse route support" should {
    "fingerprint assets" in new WithApplication() {
      router.controllers.routes.Assets.versioned("css/main.css").url must_== "/public/css/abcd1234-main.css"
    }
    "selected the minified version" in new WithApplication() {
      router.controllers.routes.Assets.versioned("css/minmain.css").url must_== "/public/css/abcd1234-minmain-min.css"
    }
    "work for non fingerprinted assets" in new WithApplication() {
      router.controllers.routes.Assets.versioned("css/nonfingerprinted.css").url must_== "/public/css/nonfingerprinted.css"
    }
    "selected the minified non fingerprinted version" in new WithApplication() {
      router.controllers.routes.Assets
        .versioned("css/nonfingerprinted-minmain.css")
        .url must_== "/public/css/nonfingerprinted-minmain-min.css"
    }
  }
}
