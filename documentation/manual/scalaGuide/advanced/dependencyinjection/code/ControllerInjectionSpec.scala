/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.dependencyinjection


import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable.Specification

import play.core.Router

/**
 *
 */
class ControllerInjectionSpec extends Specification {

  "Controller Injection" should {
    "return the appropriate instance" in {
      contentOf(FakeRequest("GET", "/")).trim must_==  "hello world!"
    }
  }

  def contentOf(rh: RequestHeader, router: Router.Routes = Routes) = running(FakeApplication(withGlobal = Some(Global)))(contentAsString(router.routes(rh) match {
    case e: EssentialAction => e(rh).run
  }))

}
