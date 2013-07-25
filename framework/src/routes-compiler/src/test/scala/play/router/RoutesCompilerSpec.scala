package play.router

import org.specs2.mutable.Specification
import play.router.RoutesCompiler._
import java.io.File
import org.specs2.execute.Result

object RoutesCompilerSpec extends Specification {

  "route file parser" should {

    def parseRoute(line: String) = {
      val rule = parseRule(line)
      rule must beAnInstanceOf[Route]
      rule.asInstanceOf[Route]
    }

    def parseRule(line: String) = {
      val parser = new RouteFileParser
      val result = parser.parse(line)
      def describeResult[T](result: parser.ParseResult[T]) = result match {
        case parser.NoSuccess(msg, _) => msg
        case _ => "successful"
      }
      result.successful aka describeResult(result) must_== true
      result.get.size must_== 1
      result.get.head
    }

    def parseError(line: String): Result = {
      val parser = new RouteFileParser
      val result = parser.parse(line)
      result must beAnInstanceOf[parser.NoSuccess]
    }

    "parse the HTTP method" in {
      parseRoute("GET /s p.c.m").verb must_== HttpVerb("GET")
    }

    "parse a static path" in {
      parseRoute("GET /s p.c.m").path must_== PathPattern(Seq(StaticPart("s")))
    }

    "parse a path with dynamic parts and it should be encodeable" in {
      parseRoute("GET /s/:d/s p.c.m").path must_== PathPattern(Seq(StaticPart("s/"), DynamicPart("d", "[^/]+", true), StaticPart("/s")))
    }

    "parse a path with multiple dynamic parts and it should not be encodeable" in {
      parseRoute("GET /s/*e p.c.m").path must_== PathPattern(Seq(StaticPart("s/"), DynamicPart("e", ".+", false)))
    }

    "path with regex should not be encodeable" in {
      parseRoute("GET /s/$id<[0-9]+> p.c.m").path must_== PathPattern(Seq(StaticPart("s/"), DynamicPart("id", "[0-9]+", false)))

    }

    "parse a single element package" in {
      parseRoute("GET /s p.c.m").call.packageName must_== "p"
    }

    "parse a multiple element package" in {
      parseRoute("GET /s p1.p2.c.m").call.packageName must_== "p1.p2"
    }

    "parse a controller" in {
      parseRoute("GET /s p.c.m").call.controller must_== "c"
    }

    "parse a method" in {
      parseRoute("GET /s p.c.m").call.method must_== "m"
    }

    "parse a parameterless method" in {
      parseRoute("GET /s p.c.m").call.parameters must beNone
    }

    "parse a zero argument method" in {
      parseRoute("GET /s p.c.m()").call.parameters must beSome(Seq())
    }

    "parse method with arguments" in {
      parseRoute("GET /s p.c.m(s1, s2)").call.parameters must beSome(Seq(Parameter("s1", "String", None, None), Parameter("s2", "String", None, None)))
    }

    "parse argument type" in {
      parseRoute("GET /s p.c.m(i: Int)").call.parameters.get.head.typeName must_== "Int"
    }

    "parse argument default value" in {
      parseRoute("GET /s p.c.m(i: Int ?= 3)").call.parameters.get.head.default must beSome("3")
    }

    "parse argument fixed value" in {
      parseRoute("GET /s p.c.m(i: Int = 3)").call.parameters.get.head.fixed must beSome("3")
    }

    "parse a non instantiating route" in {
      parseRoute("GET /s p.c.m").call.instantiate must_== false
    }

    "parse an instantiating route" in {
      parseRoute("GET /s @p.c.m").call.instantiate must_== true
    }

    "parse an include" in {
      val rule = parseRule("-> /s someFile")
      rule must beAnInstanceOf[Include]
      rule.asInstanceOf[Include].router must_== "someFile"
      rule.asInstanceOf[Include].prefix must_== "s"
    }

    "parse a comment with a route" in {
      parseRoute("# some comment\nGET /s p.c.m").comments must containTheSameElementsAs(Seq(Comment(" some comment")))
    }

    "throw an error for an unexpected line" in parseError("foo")
    "throw an error for an invalid path" in parseError("GET s p.c.m")
    "throw an error for no path" in parseError("GET")
    "throw an error for no method" in parseError("GET /s")
    "throw an error if no method specified" in parseError("GET /s p.c")
    "throw an error for an invalid include path" in parseError("-> s someFile")
    "throw an error if no include file specified" in parseError("-> /s")
  }

  "route file compiler" should {

    val tmp = new File("src/routes-compiler/target")

    def removeGenerated(routesName: String) {
      val generated = new File(tmp, "/%s/routes_routing.scala".format(routesName))
      if (generated.exists()) generated.delete()
    }

    "generate routes classes for route definitions that pass the checks" in {
      removeGenerated("generating")
      val f = new File("src/routes-compiler/src/test/resources/generating.routes")
      RoutesCompiler.compile(f, tmp, Seq.empty)

      val generatedRoutes = new File(tmp, "generating/routes_routing.scala")
      generatedRoutes.exists() must beTrue

      val generatedReverseRoutes = new File(tmp, "generating/routes_reverseRouting.scala")
      generatedReverseRoutes.exists() must beTrue
    }

    "check if there are no routes using overloaded handler methods" in {
      removeGenerated("duplicateHandlers")
      val f = new File("src/routes-compiler/src/test/resources/duplicateHandlers.routes")
      RoutesCompiler.compile(f, tmp, Seq.empty) must throwA [RoutesCompilationError]
    }
  }

}
