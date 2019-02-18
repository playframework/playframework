<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Testing your application with specs2

Writing tests for your application can be an involved process.  Play provides a default test framework for you, and provides helpers and application stubs to make testing your application as easy as possible.

## Overview

The location for tests is in the "test" folder.  There are two sample test files created in the test folder which can be used as templates.

You can run tests from the Play console.

* To run all tests, run `test`.
* To run only one test class, run `test-only` followed by the name of the class i.e. `test-only my.namespace.MySpec`.
* To run only the tests that have failed, run `test-quick`.
* To run tests continually, run a command with a tilde in front, i.e. `~test-quick`.
* To access test helpers such as `FakeRequest` in console, run `test:console`.

Testing in Play is based on sbt, and a full description is available in the [testing sbt](https://www.scala-sbt.org/0.13/docs/Testing.html) chapter.

## Using specs2

To use Play's specs2 support, add the Play specs2 dependency to your build as a test scoped dependency:

```scala
libraryDependencies += specs2 % Test
```

In [specs2](https://etorreborre.github.io/specs2/), tests are organized into specifications, which contain examples which run the system under test through various different code paths.

Specifications extend the [`Specification`](https://etorreborre.github.io/specs2/api/SPECS2-3.6.6/index.html#org.specs2.mutable.Specification) trait and are using the should/in format:

@[scalatest-helloworldspec](code/specs2/HelloWorldSpec.scala)

Specifications can be run in either IntelliJ IDEA (using the [Scala plugin](https://blog.jetbrains.com/scala/)) or in Eclipse (using the [Scala IDE](http://scala-ide.org/)).  Please see the [[IDE page|IDE]] for more details.

> **Note:** Due to a bug in the [presentation compiler](https://scala-ide-portfolio.assembla.com/spaces/scala-ide/support/tickets/1001843-specs2-tests-with-junit-runner-are-not-recognized-if-there-is-package-directory-mismatch#/activity/ticket:), tests must be defined in a specific format to work with Eclipse:

* The package must be exactly the same as the directory path.
* The specification must be annotated with `@RunWith(classOf[JUnitRunner])`.

Here is a valid specification for Eclipse:

@[basic-spec](code/models/UserSpec.scala)

### Matchers

When you use an example, you must return an example result. Usually, you will see a statement containing a `must`:

@[assertion-example](code/models/UserSpec.scala)

The expression that follows the `must` keyword are known as [`matchers`](https://etorreborre.github.io/specs2/guide/SPECS2-3.6.6/org.specs2.guide.Matchers.html). Matchers return an example result, typically Success or Failure.  The example will not compile if it does not return a result.

The most useful matchers are the [match results](https://etorreborre.github.io/specs2/guide/SPECS2-3.6.6/org.specs2.guide.Matchers.html#out-of-the-box). These are used to check for equality, determine the result of Option and Either, and even check if exceptions are thrown.

There are also [optional matchers](https://etorreborre.github.io/specs2/guide/SPECS2-3.6.6/org.specs2.guide.Matchers.html#optional) that allow for XML and JSON matching in tests.

### Mockito

Mocks are used to isolate unit tests against external dependencies.  For example, if your class depends on an external `DataService` class, you can feed appropriate data to your class without instantiating a `DataService` object.

[Mockito](https://github.com/mockito/mockito) is integrated into specs2 as the default [mocking library](https://etorreborre.github.io/specs2/guide/SPECS2-3.6.6/org.specs2.guide.UseMockito.html).

To use Mockito, add the following import:

@[import-mockito](code/models/UserSpec.scala)

You can mock out references to classes like so:

@[specs2-mockito-dataservice](code/specs2/ExampleMockitoSpec.scala)

@[specs2-mockito](code/specs2/ExampleMockitoSpec.scala)

Mocking is especially useful for testing the public methods of classes.  Mocking objects and private methods is possible, but considerably harder.

## Unit Testing Models

Play does not require models to use a particular database data access layer.  However, if the application uses Anorm or Slick, then frequently the Model will have a reference to database access internally.

```scala
import anorm._
import anorm.SqlParser._

case class User(id: String, name: String, email: String) {
   def roles = DB.withConnection { implicit connection =>
      ...
    }
}
```

For unit testing, this approach can make mocking out the `roles` method tricky.

A common approach is to keep the models isolated from the database and as much logic as possible, and abstract database access behind a repository layer.

@[scalatest-models](code/models/User.scala)

@[scalatest-repository](code/services/UserRepository.scala)

```scala
class AnormUserRepository extends UserRepository {
  import anorm._
  import anorm.SqlParser._

  def roles(user:User) : Set[Role] = {
    ...
  }
}
```

and then access them through services:

@[scalatest-userservice](code/services/UserService.scala)

In this way, the `isAdmin` method can be tested by mocking out the `UserRepository` reference and passing it into the service:

@[scalatest-userservicespec](code/specs2/UserServiceSpec.scala)

## Unit Testing Controllers

Since your controllers are just regular classes, you can easily unit test them using Play helpers. If your controllers depends on another classes, using [[dependency injection|ScalaDependencyInjection]] will enable you to mock these dependencies. Per instance, given the following controller:

@[scalatest-examplecontroller](code/specs2/ExampleControllerSpec.scala)

You can test it like:

@[scalatest-examplecontrollerspec](code/specs2/ExampleControllerSpec.scala)

### StubControllerComponents

The [`StubControllerComponentsFactory`](api/scala/play/api/test/StubControllerComponentsFactory.html) creates a stub [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) that can be used for unit testing a controller:

@[scalatest-stubcontrollercomponents](code/specs2/ExampleHelpersSpec.scala)

### StubBodyParser

The [`StubBodyParserFactory`](api/scala/play/api/test/StubBodyParserFactory.html) creates a stub [`BodyParser`](api/scala/play/api/mvc/BodyParser.html) that can be used for unit testing content:

@[scalatest-stubbodyparser](code/specs2/ExampleHelpersSpec.scala)

## Unit Testing Forms

Forms are also just regular classes, and can unit tested using Play's Test Helpers. Using [`FakeRequest`](api/scala/play/api/test/FakeRequest.html), you can call `form.bindFromRequest` and test for errors against any custom constraints.

To unit test form processing and render validation errors, you will want a [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) instance in implicit scope.  The default implementation of [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) is [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html):
  
You can test it like:

@[scalatest-exampleformspec](code/specs2/ExampleControllerSpec.scala)

When rendering a template that takes form helpers, you can pass in a Messages the same way, or use [`Helpers.stubMessages()`](api/scala/play/api/test/Helpers$.html#stubMessages\(messagesApi:play.api.i18n.MessagesApi,requestHeader:play.api.mvc.RequestHeader\):play.api.i18n.Messages):

@[scalatest-exampletemplatespec](code/specs2/ExampleControllerSpec.scala)

Or, if you are using a form that uses `CSRF.formField` and requires an implicit request, you can use [`MessagesRequest`](api/scala/play/api/mvc/MessagesRequest.html) in the template and use [`Helpers.stubMessagesRequest()`](api/scala/play/api/test/Helpers$.html#stubMessagesRequest\(messagesApi:play.api.i18n.MessagesApi,request:play.api.mvc.Request[play.api.mvc.AnyContentAsEmpty.type]\):play.api.mvc.MessagesRequest[play.api.mvc.AnyContentAsEmpty.type]):

@[scalatest-examplecsrftemplatespec](code/specs2/ExampleControllerSpec.scala)

## Unit Testing EssentialAction

Testing [`Action`](api/scala/play/api/mvc/Action.html) or [`Filter`](api/scala/play/api/mvc/Filter.html) can require to test an [`EssentialAction`](api/scala/play/api/mvc/EssentialAction.html) ([[more information about what an EssentialAction is|ScalaEssentialAction]])

For this, the test [`Helpers.call()`](api/scala/play/api/test/Helpers$.html#call[T]\(action:play.api.mvc.EssentialAction,rh:play.api.mvc.RequestHeader,body:T\)\(implicitw:play.api.http.Writeable[T],implicitmat:akka.stream.Materializer\):scala.concurrent.Future[play.api.mvc.Result]) can be used like that:

@[scalatest-exampleessentialactionspec](code/specs2/ExampleEssentialActionSpec.scala)

## Unit Testing Messages

For unit testing purposes, [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html) can be instantiated without arguments, and will take a raw map, so you can test forms and validation failures against custom [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html):

@[scalatest-examplemessagesspec](code/specs2/ExampleMessagesSpec.scala)

You can also use [`Helpers.stubMessagesApi()`](api/scala/play/api/test/Helpers$.html#stubMessagesApi\(messages:Map[String,Map[String,String]],langs:play.api.i18n.Langs,langCookieName:String,langCookieSecure:Boolean,langCookieHttpOnly:Boolean,httpConfiguration:play.api.http.HttpConfiguration\):play.api.i18n.MessagesApi) in testing to provide a premade empty [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html).
