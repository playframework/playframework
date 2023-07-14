<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Writing functional tests

Play provides a number of classes and convenience methods that assist with functional testing. 

For scala code, helpers can be found in the [`play.test`](api/java/play/test/package-summary.html) package or in the [`Helpers`](api/java/play/test/Helpers.html) class.

Depending on framework, additional helpers are provided:

- [JUnit 5](https://junit.org/junit5/): [`play.test.junit5`](api/java/play/test/junit5/package-summary.html)
- [JUnit 4](https://junit.org/junit4/): [`play.test.junit4`](api/java/play/test/junit4/package-summary.html)

# [JUnit 5](https://junit.org/junit5/) test helpers

You can add these methods and classes by importing the following:

@[test-imports](code/javaguide/test/junit5/FakeApplicationTest.java)

## Creating `Application` instances for testing

Play frequently requires a running [`Application`](api/java/play/Application.html) as context. To provide an environment for tests, Play provides helpers that produce new application instances for testing:

```java
import static play.test.Helpers.*;
```

@[test-fakeapp](code/javaguide/test/junit5/FakeApplicationTest.java)

## Testing with an application

To run tests with an `Application`, you can do the following:

@[test-running-fakeapp](code/javaguide/test/junit5/FakeApplicationTest.java)

## Injecting tests

If you're using Guice for [[dependency injection|JavaDependencyInjection]] then an `Application` for testing can be [[built directly|JavaTestingWithGuice]]. You can also inject any members of a test class that you might need. It's generally best practice to inject members only in functional tests and to manually create instances in unit tests.

@[test-injection](code/javaguide/test/junit5/InjectionTest.java)

## Testing with a Guice application

To run tests with an `Application` [[created by Guice|JavaTestingWithGuice]], you can do the following:

@[test-guiceapp](code/javaguide/test/junit5/guice/JavaGuiceApplicationBuilderTest.java)

Note that there are different ways to customize the `Application` creation when using Guice to test.

# [JUnit 4](https://junit.org/junit4/) Test Helpers

## Testing with an application

To run JUnit 4 tests with an application, one can extend [`WithApplication`](api/java/play/test/junit4/WithApplication.html).

This will automatically ensure that an application is started and stopped for each test method:

@[test-withapp](code/javaguide/test/junit4/FunctionalTest.java)

## Testing a Controller Action through Routing

With a running application, you can retrieve an action reference from the path for a route and invoke it. This also allows you to use `RequestBuilder` which creates a fake request:

@[bad-route-import](code/javaguide/test/junit4/FunctionalTest.java)

@[bad-route](code/javaguide/test/junit4/FunctionalTest.java)

It is also possible to create the `RequestBuilder` using the reverse router directly and avoid hard-coding the router path:

@[good-route](code/javaguide/test/junit4/FunctionalTest.java)

> **Note:** the reverse router is not executing the action, but instead only providing a `Call` with information that will be used to create the `RequestBuilder` and later invoke the the action itself using `Helpers.route(Application, RequestBuilder)`. That is why it is not necessary to pass a `Http.Request` when using the reverse router to create the `Http.RequestBuilder` in tests even if the action is receiving a `Http.Request` as a parameter.

## Testing with a server

Sometimes you want to test the real HTTP stack from within your test. You can do this by starting a test server:

@[test-server](code/javaguide/test/junit4/FunctionalTest.java)

Just as there exists a `WithApplication` class, there is also a [`WithServer`](api/java/play/test/junit4/WithBrowser.html) which you can extend to automatically start and stop a [`TestServer`](api/java/play/test/TestServer.html) for your tests:

@[test-withserver](code/javaguide/test/junit4/ServerFunctionalTest.java)

## Testing with a browser

If you want to test your application from with a Web browser, you can use [Selenium WebDriver](https://github.com/seleniumhq/selenium). Play will start the WebDriver for you, and wrap it in the convenient API provided by [FluentLenium](https://github.com/FluentLenium/FluentLenium).

@[test-browser](code/javaguide/test/junit4/FunctionalTest.java)

And, of course there, is the [`WithBrowser`](api/java/play/test/junit4/WithBrowser.html) class to automatically open and close a browser for each test:

@[test-withbrowser](code/javaguide/test/junit4/BrowserFunctionalTest.java)
