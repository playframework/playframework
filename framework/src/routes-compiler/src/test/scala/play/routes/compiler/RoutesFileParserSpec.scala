/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.routes.compiler

import java.io.File

import org.specs2.execute.Result
import org.specs2.mutable.Specification

object RoutesFileParserSpec extends Specification {

  "route file parser" should {

    def parseRoute(line: String) = {
      val rule = parseRule(line)
      rule must beAnInstanceOf[Route]
      rule.asInstanceOf[Route]
    }

    def parseRule(line: String): Rule = {
      val result = RoutesFileParser.parseContent(line, new File("routes"))
      result must beRight[Any]
      val rules = result.right.get
      rules.length must_== 1
      rules.head
    }

    def parseError(line: String): Result = {
      val result = RoutesFileParser.parseContent(line, new File("routes"))
      result match {
        case Left(errors) => ok
        case Right(rules) => ko("Routes compilation was successful, expected error")
      }
    }

    "parse the HTTP method" in {
      parseRoute("GET /s p.c.m").verb must_== HttpVerb("GET")
    }

    "parse a static path" in {
      parseRoute("GET /s p.c.m").path must_== PathPattern(Seq(StaticPart("s")))
    }

    "parse a path with dynamic parts and it should be encodeable" in {
      parseRoute("GET /s/:d/s p.c.m(d)").path must_== PathPattern(Seq(StaticPart("s/"), DynamicPart("d", "[^/]+", true), StaticPart("/s")))
    }

    "parse a path with multiple dynamic parts and it should not be encodeable" in {
      parseRoute("GET /s/*e p.c.m(e)").path must_== PathPattern(Seq(StaticPart("s/"), DynamicPart("e", ".+", false)))
    }

    "path with regex should not be encodeable" in {
      parseRoute("GET /s/$id<[0-9]+> p.c.m(id)").path must_== PathPattern(Seq(StaticPart("s/"), DynamicPart("id", "[0-9]+", false)))

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
      parseRoute("GET /s p.c.m()").call.parameters must_== Some(Seq())
    }

    "parse method with arguments" in {
      parseRoute("GET /s p.c.m(s1, s2)").call.parameters must_== Some(Seq(Parameter("s1", "String", None, None), Parameter("s2", "String", None, None)))
    }

    "parse method with more than 22 arguments" in {
      parseRoute("GET /s p.c.m(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int, i: Int, j: Int, k: String, l: String, m: String, n: String, " +
        "o: String, p: String, q: Option[Int], r: Option[Int], s: Option[Int], t: Option[Int], u: Option[String], v: Float, w: Float, x: Int)").call.parameters must_==
        Some(Seq(Parameter("a", "Int", None, None), Parameter("b", "Int", None, None), Parameter("c", "Int", None, None), Parameter("d", "Int", None, None),
          Parameter("e", "Int", None, None), Parameter("f", "Int", None, None), Parameter("g", "Int", None, None), Parameter("h", "Int", None, None),
          Parameter("i", "Int", None, None), Parameter("j", "Int", None, None), Parameter("k", "String", None, None), Parameter("l", "String", None, None),
          Parameter("m", "String", None, None), Parameter("n", "String", None, None), Parameter("o", "String", None, None), Parameter("p", "String", None, None),
          Parameter("q", "Option[Int]", None, None), Parameter("r", "Option[Int]", None, None), Parameter("s", "Option[Int]", None, None),
          Parameter("t", "Option[Int]", None, None), Parameter("u", "Option[String]", None, None), Parameter("v", "Float", None, None),
          Parameter("w", "Float", None, None), Parameter("x", "Int", None, None)))
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

    "parse argument with complex name" in {
      parseRoute("GET /s p.c.m(`b[]`: List[String] ?= [])").call.parameters must_== Some(Seq(
        Parameter("`b[]`", "List[String]", None, Some("[]"))))
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

}
