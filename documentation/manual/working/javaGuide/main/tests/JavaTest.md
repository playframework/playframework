<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Testing your application

Writing tests for your application can be an involved process. Play supports JUnit and provides helpers and application stubs to make testing your application as easy as possible.

## Overview

The location for tests is in the "test" folder. There are two sample test files created in the test folder which can be used as templates.

You can run tests from the Activator console.

* To run all tests, run `test`.
* To run only one test class, run `testOnly` followed by the name of the class i.e. `testOnly my.namespace.MyTest`.
* To run only the tests that have failed, run `testQuick`.
* To run tests continually, run a command with a tilde in front, i.e. `~testQuick`.
* To access test helpers such as `FakeApplication` in console, run `test:console`.

Testing in Play is based on [sbt](http://www.scala-sbt.org/), and a full description is available in the [testing documentation](http://www.scala-sbt.org/release/docs/Detailed-Topics/Testing.html).

## Using JUnit

The default way to test a Play application is with [JUnit](http://www.junit.org/).

@[test-simple](code/javaguide/tests/SimpleTest.java)

> **Note:** A new process is forked each time `test` or `test-only` is run.  The new process uses default JVM settings.  Custom settings can be added to `build.sbt`.  For example:

> ```scala
> javaOptions in Test ++= Seq(
>   "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9998",
>   "-Xms512M",
>   "-Xmx1536M",
>   "-Xss1M",
>   "-XX:MaxPermSize=384M"
> )
> ```

### Assertions & Matchers

Some developers prefer to write their assertions in a more fluent style than JUnit asserts. Popular libraries for other assertion styles are included for convenience.

Hamcrest matchers:

@[test-hamcrest](code/javaguide/tests/HamcrestTest.java)

### Mocks

Mocks are used to isolate unit tests against external dependencies. For example, if your class under test depends on an external data access class, you can mock this to provide controlled data and eliminate the need for an external data resource.

The [Mockito](https://code.google.com/p/mockito/) library is included in your project build to assist you in using mocks.

Using Mockito, you can mock classes or interfaces like so:

@[test-mockito-import](code/javaguide/tests/MockitoTest.java)

@[test-mockito](code/javaguide/tests/MockitoTest.java)

## Unit testing models

Let's assume we have the following data model:

@[test-model](code/javaguide/tests/ModelTest.java)

Some data access libraries such as Ebean allow you to put data access logic directly in your model classes using static methods. This can make mocking a data dependency tricky.

A common approach for testability is to keep the models isolated from the database and as much logic as possible, and abstract database access behind a repository interface.

@[test-model-repository](code/javaguide/tests/ModelTest.java)

Then use a service that contains your repository to interact with your models:

@[test-model-service](code/javaguide/tests/ModelTest.java)

In this way, the `UserService.isAdmin` method can be tested by mocking the `UserRepository` dependency:

@[test-model-test](code/javaguide/tests/ModelTest.java)

> **Note:** Applications using Ebean ORM may be written to rely on Play's automatic getter/setter generation.  Play also rewrites field accesses to use the generated getters/setters.  Ebean relies on calls to the setters to do dirty checking.  In order to use these patterns in JUnit tests, you will need to enable Play's field access rewriting in test by adding the following to `build.sbt`:

> ```scala
> compile in Test <<= PostCompile(Test)
> ```
>
> You may also need the following import at the top of your `build.sbt`:
>
> ```scala
> import play.Play._
> ```

## Unit testing controllers

You can test your controllers using Play's [test helpers](api/java/play/test/Helpers.html) to extract useful properties.

@[test-controller-test](code/javaguide/tests/ApplicationTest.java)

You can also retrieve an action reference from the reverse router and invoke it. This also allows you to use `FakeRequest` which is a mock for request data:

@[test-controller-routes](code/javaguide/tests/ApplicationTest.java)

## Unit testing view templates

As a template is a standard Scala function, you can execute it from a test and check the result:

@[test-template](code/javaguide/tests/ApplicationTest.java)
