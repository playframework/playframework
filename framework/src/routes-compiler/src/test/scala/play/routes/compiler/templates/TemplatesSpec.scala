/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler.templates

import org.specs2.mutable.Specification
import play.routes.compiler._

class TemplatesSpec extends Specification {
  "javascript reverse routes" should {
    "collect parameter names with index appended" in {
      val reverseParams: Seq[(Parameter, Int)] = reverseParametersJavascript(Seq(
        route("/foobar", Seq(
          Parameter("foo", "String", Some("FOO"), None),
          Parameter("bar", "String", Some("BAR"), None))),
        route("/foobar", Seq(
          Parameter("foo", "String", None, None),
          Parameter("bar", "String", None, None)))))

      reverseParams must haveSize(2)
      reverseParams(0)._1.name must_== ("foo0")
      reverseParams(1)._1.name must_== ("bar1")
    }

    "constraints uses indexed parameters" in {
      val routes = Seq(
        route("/foobar", Seq(
          Parameter("foo", "String", Some("FOO"), None),
          Parameter("bar", "String", Some("BAR"), None))),
        route("/foobar", Seq(
          Parameter("foo", "String", None, None),
          Parameter("bar", "String", None, None))))
      val localNames = reverseLocalNames(routes.head, reverseParametersJavascript(routes))
      val constraints = javascriptParameterConstraints(routes.head, localNames)

      constraints.get must startWith("foo0 == ")
      constraints.get must contain("bar1 == ")
    }
  }

  def route(staticPath: String, params: Seq[Parameter] = Nil): Route = {
    Route(
      HttpVerb("GET"),
      PathPattern(Seq(StaticPart(staticPath))),
      HandlerCall(Option("pkg"), "ctrl", true, "method", Some(params)))
  }
}
