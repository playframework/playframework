<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Testing your application with specs2

Writing tests for your application can be an involved process.  Play provides a default test framework for you, and provides helpers and application stubs to make testing your application as easy as possible.

## Overview

The location for tests is in the "test" folder.  There are two sample test files created in the test folder which can be used as templates.

You can run tests from the Play console.

* To run all tests, run `test`.
* To run only one test class, run `test-only` followed by the name of the class i.e. `test-only my.namespace.MySpec`.
* To run only the tests that have failed, run `test-quick`.
* To run tests continually, run a command with a tilde in front, i.e. `~test-quick`.
* To access test helpers such as `FakeApplication` in console, run `test:console`.

Testing in Play is based on SBT, and a full description is available in the [testing SBT](http://www.scala-sbt.org/0.13.0/docs/Detailed-Topics/Testing) chapter.

## Using specs2

To use Play's specs2 support, add the Play specs2 dependency to your build as a test scoped dependency:

```scala
libraryDependencies += specs2 % Test
```

In [specs2](http://etorreborre.github.io/specs2/), tests are organized into specifications, which contain examples which run the system under test through various different code paths.

Specifications extend the [`Specification`](http://etorreborre.github.io/specs2/api/SPECS2-2.4.9/index.html#org.specs2.mutable.Specification) trait and are using the should/in format:

@[scalatest-helloworldspec](code/specs2/HelloWorldSpec.scala)

Specifications can be run in either IntelliJ IDEA (using the [Scala plugin](http://blog.jetbrains.com/scala/)) or in Eclipse (using the [Scala IDE](http://scala-ide.org/)).  Please see the [[IDE page|IDE]] for more details.

NOTE: Due to a bug in the [presentation compiler](https://scala-ide-portfolio.assembla.com/spaces/scala-ide/support/tickets/1001843-specs2-tests-with-junit-runner-are-not-recognized-if-there-is-package-directory-mismatch#/activity/ticket:), tests must be defined in a specific format to work with Eclipse:

* The package must be exactly the same as the directory path.
* The specification must be annotated with `@RunWith(classOf[JUnitRunner])`.

Here is a valid specification for Eclipse:

```scala
package models // this file must be in a directory called "models"

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  ...
}
```

### Matchers

When you use an example, you must return an example result. Usually, you will see a statement containing a `must`:

```scala
"Hello world" must endWith("world")
```

The expression that follows the `must` keyword are known as [`matchers`](http://etorreborre.github.io/specs2/guide/org.specs2.guide.Matchers.html). Matchers return an example result, typically Success or Failure.  The example will not compile if it does not return a result.

The most useful matchers are the [match results](http://etorreborre.github.io/specs2/guide/org.specs2.guide.Matchers.html#Match+results).  These are used to check for equality, determine the result of Option and Either, and even check if exceptions are thrown.

There are also [optional matchers](http://etorreborre.github.io/specs2/guide/org.specs2.guide.Matchers.html#Optional) that allow for XML and JSON matching in tests.

### Mockito

Mocks are used to isolate unit tests against external dependencies.  For example, if your class depends on an external `DataService` class, you can feed appropriate data to your class without instantiating a `DataService` object.

[Mockito](https://code.google.com/p/mockito/) is integrated into specs2 as the default [mocking library](http://etorreborre.github.io/specs2/guide/org.specs2.guide.Matchers.html#Mock+expectations).

To use Mockito, add the following import:

```scala
import org.specs2.mock._
```

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

Controllers are defined as objects in Play, and so can be trickier to unit test.  In Play this can be alleviated by [[dependency injection|ScalaDependencyInjection]] using [`getControllerInstance`](api/scala/index.html#play.api.GlobalSettings@getControllerInstance).  Another way to finesse unit testing with a controller is to use a trait with an [explicitly typed self reference](http://www.naildrivin5.com/scalatour/wiki_pages/ExplcitlyTypedSelfReferences) to the controller:

@[scalatest-examplecontroller](code/specs2/ExampleControllerSpec.scala)

and then test the trait:

@[scalatest-examplecontrollerspec](code/specs2/ExampleControllerSpec.scala)

## Unit Testing EssentialAction

Testing [`Action`](api/scala/index.html#play.api.mvc.Action) or [`Filter`](api/scala/index.html#play.api.mvc.Filter) can require to test an [`EssentialAction`](api/scala/index.html#play.api.mvc.EssentialAction) ([[more information about what an EssentialAction is|HttpApi]])

For this, the test [`Helpers.call`](api/scala/index.html#play.api.test.Helpers@call) can be used like that:

@[scalatest-exampleessentialactionspec](code/specs2/ExampleEssentialActionSpec.scala)
