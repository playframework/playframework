package play.api.routing

import org.specs2.mutable.Specification

class JavaScriptReverseRouterSpec extends Specification {
  "JavaScriptReverseRouter" should {
    "Create a JavaScript router with the right script" in {
      val foo = "function(foo) { return null; }"
      val bar = "function(bar) { return null; }"
      val router = JavaScriptReverseRouter(name = "lightbendRoutes", ajaxMethod = Some("doAjaxRequest"), host = "lightbend.com",
        JavaScriptReverseRoute("controllers.FooController.foo", foo),
        JavaScriptReverseRoute("controllers.BarController.bar", bar)
      )
      router.body must contain("var lightbendRoutes = ")
      router.body must contain("return doAjaxRequest(c)")
      router.body must contain("'lightbend.com'")
      router.body must contain(s"_root['controllers']['FooController']['foo'] = $foo")
      router.body must contain(s"_root['controllers']['BarController']['bar'] = $bar")
    }
  }
}
