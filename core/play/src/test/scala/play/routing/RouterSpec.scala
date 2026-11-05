/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing

import java.util.Optional

import org.specs2.mutable.Specification
import play.api.mvc.Handler
import play.core.test.FakeRequest
import play.mvc.Http.RequestHeader

class RouterSpec extends Specification {
  "Router.asScala" should {
    "preserve the Java router's definedness and perform one lookup in applyOrElse" in {
      object Matched  extends Handler
      object Fallback extends Handler

      var routeCalls = 0
      val router     = new Router {
        override def documentation(): java.util.List[Router.RouteDocumentation] = java.util.List.of()

        override def route(request: RequestHeader): Optional[Handler] = {
          routeCalls += 1
          if (request.path() == "/matched") Optional.of[Handler](Matched) else Optional.empty()
        }

        override def withPrefix(prefix: String): Router = this
      }

      val routes  = router.asScala().routes
      val matched = FakeRequest("GET", "/matched")
      val missing = FakeRequest("GET", "/missing")

      routes.isDefinedAt(matched) must beTrue
      routes.isDefinedAt(missing) must beFalse

      routeCalls = 0
      val matchedResult: Handler = routes.applyOrElse(matched, (_: play.api.mvc.RequestHeader) => Fallback)
      matchedResult must beTheSameAs(Matched)
      routeCalls must_== 1

      routeCalls = 0
      val missingResult: Handler = routes.applyOrElse(missing, (_: play.api.mvc.RequestHeader) => Fallback)
      missingResult must beTheSameAs(Fallback)
      routeCalls must_== 1

      routeCalls = 0
      routes(missing) must throwA[MatchError]
      routeCalls must_== 1
    }
  }
}
