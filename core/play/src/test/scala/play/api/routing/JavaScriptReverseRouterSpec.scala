/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.routing

import org.specs2.mutable.Specification
import play.api.http.HeaderNames.HOST
import play.api.mvc.RequestHeader
import play.core.test.FakeRequest

class JavaScriptReverseRouterSpec extends Specification {
  "JavaScriptReverseRouter" should {
    val foo = JavaScriptReverseRoute("controllers.FooController.foo", "function(foo) { return null; }")
    val bar = JavaScriptReverseRoute("controllers.BarController.bar", "function(bar) { return null; }")

    "create a JavaScript router without an ajax helper using an explicit host" in {
      val router = JavaScriptReverseRouter("foobarRoutes", "foobar.com", foo, bar)

      router.body must contain("var foobarRoutes = ")
      router.body must not(contain("ajax:function"))
      router.body must contain("'foobar.com'")
      router.body must contain(s"_root['controllers']['FooController']['foo'] = ${foo.f}")
      router.body must contain(s"_root['controllers']['BarController']['bar'] = ${bar.f}")
    }

    "use the request host without adding an ajax helper" in {
      implicit val request: RequestHeader = FakeRequest().withHeaders(HOST -> "request.example")

      val router = JavaScriptReverseRouter("foobarRoutes")(foo)

      router.body must contain("'request.example'")
      router.body must not(contain("ajax:function"))
    }

    "preserve custom ajax helpers through the deprecated overload" in {
      val foo    = "function(foo) { return null; }"
      val bar    = "function(bar) { return null; }"
      val router = JavaScriptReverseRouter(
        name = "foobarRoutes",
        ajaxMethod = Some("doAjaxRequest"),
        host = "foobar.com",
        JavaScriptReverseRoute("controllers.FooController.foo", foo),
        JavaScriptReverseRoute("controllers.BarController.bar", bar)
      )
      router.body must contain("var foobarRoutes = ")
      router.body must contain("return doAjaxRequest(c)")
      router.body must contain("'foobar.com'")
      router.body must contain(s"_root['controllers']['FooController']['foo'] = $foo")
      router.body must contain(s"_root['controllers']['BarController']['bar'] = $bar")
    }
  }
}
