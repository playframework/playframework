<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Testing your application

Writing tests for your application can be an involved process. Play supports [JUnit](https://junit.org/junit4/) and provides helpers and application stubs to make testing your application as easy as possible.

## Overview

The location for tests is in the "test" folder. There are two sample test files created in the test folder which can be used as templates.

You can run tests from the sbt console.

* To run all tests, run `test`.
* To run only one test class, run `testOnly` followed by the name of the class i.e. `testOnly my.namespace.MyTest`.
* To run only the tests that have failed, run `testQuick`.
* To run tests continually, run a command with a tilde in front, i.e. `~testQuick`.
* To access test helpers such as `FakeApplication` in console, run `test:console`.

Testing in Play is based on [sbt](https://www.scala-sbt.org/), and a full description is available in the [testing documentation](https://www.scala-sbt.org/release/docs/Testing.html).

## Using JUnit

The default way to test a Play application is with [JUnit](https://junit.org/junit4/).

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

[Hamcrest](http://hamcrest.org/JavaHamcrest/) matchers:

@[test-hamcrest](code/javaguide/tests/HamcrestTest.java)

### Mocks

Mocks are used to isolate unit tests against external dependencies. For example, if your class under test depends on an external data access class, you can mock this to provide controlled data and eliminate the need for an external data resource.

The [Mockito](https://github.com/mockito/mockito) library is a popular mocking framework for Java.  To use it in your tests add a dependency on the `mockito-core` artifact to your `build.sbt` file.  For example:

```scala
libraryDependencies += "org.mockito" % "mockito-core" % "2.10.0" % "test"
```

You can find the current version number of `mockito-core` [here](https://mvnrepository.com/artifact/org.mockito/mockito-core).

Using Mockito, you can mock classes or interfaces like so:

@[test-mockito-import](code/javaguide/tests/MockitoTest.java)

@[test-mockito](code/javaguide/tests/MockitoTest.java)

## Unit testing models

Let's assume we have the following data model:

@[test-model](code/javaguide/tests/ModelTest.java)

Some data access libraries such as [Ebean](http://ebean-orm.github.io/) allow you to put data access logic directly in your model classes using static methods. This can make mocking a data dependency tricky.

A common approach for testability is to keep the models isolated from the database and as much logic as possible, and abstract database access behind a repository interface.

@[test-model-repository](code/javaguide/tests/ModelTest.java)

Then use a service that contains your repository to interact with your models:

@[test-model-service](code/javaguide/tests/ModelTest.java)

In this way, the `UserService.isAdmin` method can be tested by mocking the `UserRepository` dependency:

@[test-model-test](code/javaguide/tests/ModelTest.java)

> **Note:** Applications using Ebean ORM may be written to rely on Play's automatic getter/setter generation. If this is your case, check how [[Play enhancer sbt plugin|PlayEnhancer]] works.

## Unit testing controllers

You can test your controllers using Play's [test helpers](api/java/play/test/Helpers.html) to extract useful properties.

@[test-controller-test](code/javaguide/tests/ControllerTest.java)

## Unit testing view templates

As a template is a just a method, you can execute it from a test and check the result:

@[test-template](code/javaguide/tests/ControllerTest.java)

## Unit testing with Messages

If you need a `play.i18n.MessagesApi` instance for unit testing, you can use [`play.test.Helpers.stubMessagesApi()`](api/java/play/test/Helpers.html#stubMessagesApi-java.util.Map-play.i18n.Langs-) to provide one:

@[test-messages](code/javaguide/tests/MessagesTest.java)
