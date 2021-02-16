/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package test

import play.api.test._

import java.util.Optional
import java.util.UUID
import models.UserId
import scala.concurrent.Future

object RouterSpec extends PlaySpecification {

  "path binding reverse routing" should {
    "add parameters to the path where path param is of type String" in {
      controllers.routes.Application.withParam("foo").url must equalTo("/with/foo")
    }
    "handle default path param of type String" in {
      controllers.routes.Application.withParam("beer").url must equalTo("/with")
    }
    "add parameters to the path where path param is of type Option[String]" in {
      controllers.routes.Application.withParamOption(Some("fooOpt")).url must equalTo("/withOpt/fooOpt")
    }
    "handle path param None of type Option[String]" in {
      controllers.routes.Application.withParamOption(None).url must equalTo("/withOptNone")
    }
    "handle default path param of type Option[String]" in {
      controllers.routes.Application.withParamOption(Some("wine")).url must equalTo("/withOpt")
    }
    "add parameters to the path where path param is of type JOptional[String]" in {
      controllers.routes.Application.withParamOptional(Optional.of("fooJOpt")).url must equalTo("/withJOpt/fooJOpt")
    }
    "handle path param Empty of type JOptional[String]" in {
      controllers.routes.Application.withParamOptional(Optional.empty()).url must equalTo("/withJOptEmpty")
    }
    "handle default path param of type JOptional[String]" in {
      controllers.routes.Application.withParamOptional(Optional.of("coffee")).url must equalTo("/withJOpt")
    }
    "add parameters to the path where path param is of type UUID" in {
      controllers.routes.Application.withUUIDParam(UUID.fromString("7c815c5a-d112-4a69-a6c7-a0fa32361fda")).url must equalTo("/withUUID/7c815c5a-d112-4a69-a6c7-a0fa32361fda")
    }
    "handle default path param of type UUID" in {
      controllers.routes.Application.withUUIDParam(UUID.fromString("11111111-1111-1111-1111-111111111111")).url must equalTo("/withUUID")
    }
    "add parameters to the path where path param is of type Option[UUID]" in {
      controllers.routes.Application.withUUIDParamOption(Some(UUID.fromString("8bd1b515-e269-48cd-8af2-09e093fee383"))).url must equalTo("/withUUIDOpt/8bd1b515-e269-48cd-8af2-09e093fee383")
    }
    "handle path param None of type Option[UUID]" in {
      controllers.routes.Application.withUUIDParamOption(None).url must equalTo("/withUUIDOptNone")
    }
    "handle default path param of type Option[UUID]" in {
      controllers.routes.Application.withUUIDParamOption(Some(UUID.fromString("22222222-2222-2222-2222-222222222222"))).url must equalTo("/withUUIDOpt")
    }
    "add parameters to the path where path param is of type JOptional[UUID]" in {
      controllers.routes.Application.withUUIDParamOptional(Optional.of(UUID.fromString("a0801c7f-e5a5-4964-bfae-78fb85e21f72"))).url must equalTo("/withUUIDJOpt/a0801c7f-e5a5-4964-bfae-78fb85e21f72")
    }
    "handle path param Empty of type JOptional[UUID]" in {
      controllers.routes.Application.withUUIDParamOptional(Optional.empty()).url must equalTo("/withUUIDJOptEmpty")
    }
    "handle default path param of type JOptional[UUID]" in {
      controllers.routes.Application.withUUIDParamOptional(Optional.of(UUID.fromString("33333333-3333-3333-3333-333333333333"))).url must equalTo("/withUUIDJOpt")
    }
    "add parameters to the path where path param is of type UserId" in {
      controllers.routes.Application.user(UserId("carl")).url must equalTo("/users/carl")
    }
    "handle default path param of type UserId" in {
      controllers.routes.Application.user(UserId("123")).url must equalTo("/user")
    }
    "add parameters to the path where path param is of type Option[UserId]" in {
      controllers.routes.Application.userOption(Some(UserId("john"))).url must equalTo("/usersOpt/john")
    }
    "handle path param None of type Option[UserId]" in {
      controllers.routes.Application.userOption(None).url must equalTo("/usersOptNone")
    }
    "handle default path param of type Option[UserId]" in {
      controllers.routes.Application.userOption(Some(UserId("abc"))).url must equalTo("/usersOpt")
    }
    "add parameters to the path where path param is of type Optional[UserId]" in {
      controllers.routes.Application.userOptional(Optional.of(UserId("james"))).url must equalTo("/usersJOpt/james")
    }
    "handle path param Empty of type JOptional[UserId]" in {
      controllers.routes.Application.userOptional(Optional.empty()).url must equalTo("/usersJOptEmpty")
    }
    "handle default path param of type JOptional[UserId]" in {
      controllers.routes.Application.userOptional(Optional.of(UserId("xyz"))).url must equalTo("/usersJOpt")
    }
  }

  "path binding should bind variables" in new WithApplication() {
    contentAsString(route(implicitApp, FakeRequest(GET, "/with")).get) must equalTo("beer")
    contentAsString(route(implicitApp, FakeRequest(GET, "/with/fooasdf")).get) must equalTo("fooasdf")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withOptNone")).get) must equalTo("<none>")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withOpt")).get) must equalTo("Option: wine")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withOpt/fooxyz")).get) must equalTo("Option: fooxyz")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withJOptEmpty")).get) must equalTo("<empty>")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withJOpt")).get) must equalTo("JOptional: coffee")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withJOpt/foo123")).get) must equalTo("JOptional: foo123")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUID")).get) must equalTo("11111111-1111-1111-1111-111111111111")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUID/44444444-4444-4444-4444-444444444444")).get) must equalTo("44444444-4444-4444-4444-444444444444")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUIDOptNone")).get) must equalTo("<uuid_none>")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUIDOpt")).get) must equalTo("Option: 22222222-2222-2222-2222-222222222222")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUIDOpt/55555555-5555-5555-5555-555555555555")).get) must equalTo("Option: 55555555-5555-5555-5555-555555555555")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUIDJOptEmpty")).get) must equalTo("<uuid_empty>")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUIDJOpt")).get) must equalTo("JOptional: 33333333-3333-3333-3333-333333333333")
    contentAsString(route(implicitApp, FakeRequest(GET, "/withUUIDJOpt/66666666-6666-6666-6666-666666666666")).get) must equalTo("JOptional: 66666666-6666-6666-6666-666666666666")
    contentAsString(route(implicitApp, FakeRequest(GET, "/user")).get) must equalTo("123")
    contentAsString(route(implicitApp, FakeRequest(GET, "/users/qwertz")).get) must equalTo("qwertz")
    contentAsString(route(implicitApp, FakeRequest(GET, "/usersOptNone")).get) must equalTo("<user_none>")
    contentAsString(route(implicitApp, FakeRequest(GET, "/usersOpt")).get) must equalTo("Option: abc")
    contentAsString(route(implicitApp, FakeRequest(GET, "/usersOpt/jkl")).get) must equalTo("Option: jkl")
    contentAsString(route(implicitApp, FakeRequest(GET, "/usersJOptEmpty")).get) must equalTo("<user_empty>")
    contentAsString(route(implicitApp, FakeRequest(GET, "/usersJOpt")).get) must equalTo("JOptional: xyz")
    contentAsString(route(implicitApp, FakeRequest(GET, "/usersJOpt/fdsa")).get) must equalTo("JOptional: fdsa")
  }

  "reverse routes containing boolean parameters" in {
    "the query string" in {
      controllers.routes.Application.takeBool(true).url must equalTo("/take-bool?b=true")
      controllers.routes.Application.takeBool(false).url must equalTo("/take-bool?b=false")
    }
    "the path" in {
      controllers.routes.Application.takeBool2(true).url must equalTo("/take-bool-2/true")
      controllers.routes.Application.takeBool2(false).url must equalTo("/take-bool-2/false")
    }
  }

  "reverse routes containing custom parameters" in {
    "the query string" in {
      controllers.routes.Application.queryUser(UserId("foo")).url must equalTo("/query-user?userId=foo")
      controllers.routes.Application.queryUser(UserId("foo/bar")).url must equalTo("/query-user?userId=foo%2Fbar")
      controllers.routes.Application.queryUser(UserId("foo?bar")).url must equalTo("/query-user?userId=foo%3Fbar")
      controllers.routes.Application.queryUser(UserId("foo%bar")).url must equalTo("/query-user?userId=foo%25bar")
      controllers.routes.Application.queryUser(UserId("foo&bar")).url must equalTo("/query-user?userId=foo%26bar")
    }
    "the path" in {
      controllers.routes.Application.user(UserId("foo")).url must equalTo("/users/foo")
      controllers.routes.Application.user(UserId("foo/bar")).url must equalTo("/users/foo%2Fbar")
      controllers.routes.Application.user(UserId("foo?bar")).url must equalTo("/users/foo%3Fbar")
      controllers.routes.Application.user(UserId("foo%bar")).url must equalTo("/users/foo%25bar")
      // & is not special for path segments
      controllers.routes.Application.user(UserId("foo&bar")).url must equalTo("/users/foo&bar")
    }
  }

  "bind boolean parameters" in {
    "from the query string" in new WithApplication() {
      val Some(result) = route(implicitApp, FakeRequest(GET, "/take-bool?b=true"))
      contentAsString(result) must equalTo("true")
      val Some(result2) = route(implicitApp, FakeRequest(GET, "/take-bool?b=false"))
      contentAsString(result2) must equalTo("false")
      // Bind boolean values from 1 and 0 integers too
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool?b=1")).get) must equalTo("true")
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool?b=0")).get) must equalTo("false")
    }
    "from the path" in new WithApplication() {
      val Some(result) = route(implicitApp, FakeRequest(GET, "/take-bool-2/true"))
      contentAsString(result) must equalTo("true")
      val Some(result2) = route(implicitApp, FakeRequest(GET, "/take-bool-2/false"))
      contentAsString(result2) must equalTo("false")
      // Bind boolean values from 1 and 0 integers too
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool-2/1")).get) must equalTo("true")
      contentAsString(route(implicitApp, FakeRequest(GET, "/take-bool-2/0")).get) must equalTo("false")
    }
  }

  "bind int parameters from the query string as a list" in {

    "from a list of numbers" in new WithApplication() {
      val Some(result) =
        route(implicitApp, FakeRequest(GET, controllers.routes.Application.takeListInt(List(1, 2, 3)).url))
      contentAsString(result) must equalTo("1,2,3")
    }
    "from a list of numbers and letters" in new WithApplication() {
      val Some(result) = route(implicitApp, FakeRequest(GET, "/take-slist-int?x=1&x=a&x=2"))
      status(result) must equalTo(BAD_REQUEST)
    }
    "when there is no parameter at all" in new WithApplication() {
      val Some(result) = route(implicitApp, FakeRequest(GET, "/take-slist-int"))
      contentAsString(result) must equalTo("")
    }
    "using the Java API" in new WithApplication() {
      val Some(result) = route(implicitApp, FakeRequest(GET, "/take-jlist-jint?x=1&x=2&x=3"))
      contentAsString(result) must equalTo("1,2,3")
    }
  }

  private def testQueryParamBindingWithDefault(
      paramType: String,
      path: String,
      successParams: String,
      expectationSuccess: String,
      whenNoValue: Future[play.api.mvc.Result] => Any,
      whenNoParam: Future[play.api.mvc.Result] => Any
  ) =
    testQueryParamBinding(paramType, path, successParams, expectationSuccess, whenNoValue, whenNoParam, true)

  private def testQueryParamBinding(
      paramType: String,
      path: String,
      successParams: String,
      successExpectation: String,
      whenNoValue: Future[play.api.mvc.Result] => Any,
      whenNoParam: Future[play.api.mvc.Result] => Any,
      withDefault: Boolean = false
  ) = {
    lazy val resolvedPath = s"/${path}${if (withDefault) "-d" else ""}"
    s"bind ${paramType} parameter${if (withDefault) " with default value" else ""} from the query string" in {
      "successfully" in new WithApplication() {
        val Some(result) = route(implicitApp, FakeRequest(GET, s"${resolvedPath}?${successParams}"))
        contentAsString(result) must equalTo(successExpectation)
        status(result) must equalTo(OK)
      }
      "when there is a parameter but without value (=empty string)" in new WithApplication() {
        val Some(result) = route(implicitApp, FakeRequest(GET, s"${resolvedPath}?x="))
        whenNoValue(result)
      }
      "when there is a parameter but without value (=empty string) and without equals sign" in new WithApplication() {
        val Some(result) = route(implicitApp, FakeRequest(GET, s"${resolvedPath}?x"))
        whenNoValue(result)
      }
      "when there is no parameter at all" in new WithApplication() {
        val Some(result) = route(implicitApp, FakeRequest(GET, resolvedPath))
        whenNoParam(result)
      }
    }
  }

  testQueryParamBinding(
    "String",
    "take-str",
    "x=xyz",
    "xyz", // calls takeString(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must contain("Missing parameter: x")
      status(result) must equalTo(BAD_REQUEST)
    }
  )
  testQueryParamBinding(
    "Option[String]",
    "take-str-opt",
    "x=xyz",
    "xyz", // calls takeStringOption(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("emptyOption")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "java.util.Optional[String]",
    "take-str-jopt",
    "x=xyz",
    "xyz", // calls takeStringOptional(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("emptyOptional")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "Char",
    "take-char",
    "x=z",
    "z", // calls takeChar(...)
    whenNoValue = result => {
      contentAsString(result) must contain("Missing parameter: x")
      status(result) must equalTo(BAD_REQUEST)
    },
    whenNoParam = result => {
      contentAsString(result) must contain("Missing parameter: x")
      status(result) must equalTo(BAD_REQUEST)
    }
  )
  testQueryParamBinding(
    "Option[Char]",
    "take-char-opt",
    "x=z",
    "z", // calls takeCharOption(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyOption")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("emptyOption")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "Int",
    "take-int",
    "x=789",
    "789", // calls takeInt(...)
    whenNoValue = result => {
      contentAsString(result) must contain("Missing parameter: x")
      status(result) must equalTo(BAD_REQUEST)
    },
    whenNoParam = result => {
      contentAsString(result) must contain("Missing parameter: x")
      status(result) must equalTo(BAD_REQUEST)
    }
  )
  testQueryParamBinding(
    "Option[Int]",
    "take-int-opt",
    "x=789",
    "789", // calls takeIntOption(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyOption")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("emptyOption")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "java.lang.Integer",
    "take-jint",
    "x=789",
    "789", // calls takeInteger(...)
    whenNoValue = result => {
      contentAsString(result) must contain("Missing parameter: x")
      status(result) must equalTo(BAD_REQUEST)
    },
    whenNoParam = result => {
      contentAsString(result) must contain("Missing parameter: x")
      status(result) must equalTo(BAD_REQUEST)
    }
  )
  testQueryParamBinding(
    "java.util.Optional[java.lang.Integer]",
    "take-jint-jopt",
    "x=789",
    "789", // calls takeIntegerOptional(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyOptional")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("emptyOptional")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "List[String]",
    "take-slist-str",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeListString(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty List("") was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty List() was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "Option[List[String]]",
    "take-slist-str-opt",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeListStringOption(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty list Some(List("")) was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty list Some(List()) was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "List[Char]",
    "take-slist-char",
    "x=z",
    "z", // calls takeListChar(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("") // means empty List() was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty List() was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "Option[List[Char]]",
    "take-slist-char-opt",
    "x=z",
    "z", // calls takeListCharOption(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("") // means empty Some(List()) was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty Some(List()) was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "java.util.List[String]",
    "take-jlist-str",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeJavaListString(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty List("") was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty List() was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "java.util.Optional[java.util.List[String]]",
    "take-jlist-str-jopt",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeJavaListStringOptional(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty list Optinal.of(List("")) was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty list Optinal.of(List()) was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "List[Int]",
    "take-slist-int",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeListInt(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("") // means empty List() was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty List() was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "Option[List[Int]]",
    "take-slist-int-opt",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeListIntOption(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("") // means empty list Some(List()) was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty list Some(List()) was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "java.util.List[java.lang.Integer]",
    "take-jlist-jint",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeJavaListInteger(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("") // means empty list List() was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty list List() was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBinding(
    "java.util.Optional[java.util.List[java.lang.Integer]]",
    "take-jlist-jint-jopt",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeJavaListIntegerOptional(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("") // means empty list Optional.of(List()) was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("") // means empty list Optional.of(List()) was passed to action
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "String",
    "take-str",
    "x=xyz",
    "xyz", // calls takeStringWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("abc")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Option[String]",
    "take-str-opt",
    "x=xyz",
    "xyz", // calls takeStringOptionWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("abc")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "java.util.Optional[String]",
    "take-str-jopt",
    "x=xyz",
    "xyz", // calls takeStringOptionalWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("abc")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Char",
    "take-char",
    "x=z",
    "z", // calls takeCharWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("a")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("a")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Option[Char]",
    "take-char-opt",
    "x=z",
    "z", // calls takeCharOptionWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("a")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("a")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Int",
    "take-int",
    "x=789",
    "789", // calls takeIntWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Option[Int]",
    "take-int-opt",
    "x=789",
    "789", // calls takeIntOptionWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "java.lang.Integer",
    "take-jint",
    "x=789",
    "789", // calls takeIntegerWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "java.util.Optional[java.lang.Integer]",
    "take-jint-jopt",
    "x=789",
    "789", // calls takeIntegerOptionalWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("123")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "List[String]",
    "take-slist-str",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeListStringWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty List("") was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("abc,def,ghi")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Option[List[String]]",
    "take-slist-str-opt",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeListStringOptionWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty list Some(List("")) was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("abc,def,ghi")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "List[Char]",
    "take-slist-char",
    "x=z",
    "z", // calls takeListCharWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("a,b,c")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("a,b,c")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Option[List[Char]]",
    "take-slist-char-opt",
    "x=z",
    "z", // calls takeListCharOptionWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("a,b,c")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("a,b,c")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "java.util.List[String]",
    "take-jlist-str",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeJavaListStringWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty List("") was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("abc,def,ghi")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "java.util.Optional[java.util.List[String]]",
    "take-jlist-str-jopt",
    "x=x&x=y&x=z",
    "x,y,z", // calls takeJavaListStringOptionalWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("emptyStringElement") // means non-empty list Optinal.of(List("")) was passed to action
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("abc,def,ghi")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "List[Int]",
    "take-slist-int",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeListIntWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "Option[List[Int]]",
    "take-slist-int-opt",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeListIntOptionWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "java.util.List[java.lang.Integer]",
    "take-jlist-jint",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeJavaListIntegerWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    }
  )
  testQueryParamBindingWithDefault(
    "java.util.Optional[java.util.List[java.lang.Integer]]",
    "take-jlist-jint-jopt",
    "x=7&x=8&x=9",
    "7,8,9", // calls takeJavaListIntegerOptionalWithDefault(...)
    whenNoValue = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    },
    whenNoParam = result => {
      contentAsString(result) must equalTo("1,2,3")
      status(result) must equalTo(OK)
    }
  )

  "URL encoding and decoding works correctly" in new WithApplication() {
    def checkDecoding(
        dynamicEncoded: String,
        staticEncoded: String,
        queryEncoded: String,
        dynamicDecoded: String,
        staticDecoded: String,
        queryDecoded: String
    ) = {
      val path         = s"/urlcoding/$dynamicEncoded/$staticEncoded?q=$queryEncoded"
      val expected     = s"dynamic=$dynamicDecoded static=$staticDecoded query=$queryDecoded"
      val Some(result) = route(implicitApp, FakeRequest(GET, path))
      val actual       = contentAsString(result)
      actual must equalTo(expected)
    }
    def checkEncoding(
        dynamicDecoded: String,
        staticDecoded: String,
        queryDecoded: String,
        dynamicEncoded: String,
        staticEncoded: String,
        queryEncoded: String
    ) = {
      val expected = s"/urlcoding/$dynamicEncoded/$staticEncoded?q=$queryEncoded"
      val call     = controllers.routes.Application.urlcoding(dynamicDecoded, staticDecoded, queryDecoded)
      call.url must equalTo(expected)
    }
    checkDecoding("a", "a", "a", "a", "a", "a")
    checkDecoding("%2B", "%2B", "%2B", "+", "%2B", "+")
    checkDecoding("+", "+", "+", "+", "+", " ")
    checkDecoding("%20", "%20", "%20", " ", "%20", " ")
    checkDecoding("&", "&", "-", "&", "&", "-")
    checkDecoding("=", "=", "-", "=", "=", "-")

    checkEncoding("+", "+", "+", "+", "+", "%2B")
    checkEncoding(" ", " ", " ", "%20", " ", "+")
    checkEncoding("&", "&", "&", "&", "&", "%26")
    checkEncoding("=", "=", "=", "=", "=", "%3D")

    // We use java.net.URLEncoder for query string encoding, which is not
    // RFC compliant, e.g. it percent-encodes "/" which is not a delimiter
    // for query strings, and it percent-encodes "~" which is an "unreserved" character
    // that should never be percent-encoded. The following tests, therefore
    // don't really capture our ideal desired behaviour for query string
    // encoding. However, the behaviour for dynamic and static paths is correct.
    checkEncoding("/", "/", "/", "%2F", "/", "%2F")
    checkEncoding("~", "~", "~", "~", "~", "%7E")

    checkDecoding("123", "456", "789", "123", "456", "789")
    checkEncoding("123", "456", "789", "123", "456", "789")
  }

  "allow reverse routing of routes includes" in new WithApplication() {
    // Force the router to bootstrap the prefix
    implicitApp.injector.instanceOf[play.api.routing.Router]
    controllers.module.routes.ModuleController.index.url must_== "/module/index"
  }

  "document the router" in new WithApplication() {
    // The purpose of this test is to alert anyone that changes the format of the router documentation that
    // it is being used by Swagger. So if you do change it, please let Tony Tam know at tony at wordnik dot com.
    val someRoute = implicitApp.injector
      .instanceOf[play.api.routing.Router]
      .documentation
      .find(r => r._1 == "GET" && r._2.startsWith("/with/"))
    someRoute must beSome[(String, String, String)]
    val route = someRoute.get
    route._2 must_== "/with/$param<[^/]+>"
    route._3 must startWith("controllers.Application.withParam")
  }

  "choose the first matching route for a call in reverse routes" in new WithApplication() {
    controllers.routes.Application.hello.url must_== "/hello"
  }

  "The assets reverse route support" should {
    "fingerprint assets" in new WithApplication() {
      controllers.routes.Assets.versioned("css/main.css").url must_== "/public/css/abcd1234-main.css"
    }
    "selected the minified version" in new WithApplication() {
      controllers.routes.Assets.versioned("css/minmain.css").url must_== "/public/css/abcd1234-minmain-min.css"
    }
    "work for non fingerprinted assets" in new WithApplication() {
      controllers.routes.Assets.versioned("css/nonfingerprinted.css").url must_== "/public/css/nonfingerprinted.css"
    }
    "selected the minified non fingerprinted version" in new WithApplication() {
      controllers.routes.Assets
        .versioned("css/nonfingerprinted-minmain.css")
        .url must_== "/public/css/nonfingerprinted-minmain-min.css"
    }
  }
}
