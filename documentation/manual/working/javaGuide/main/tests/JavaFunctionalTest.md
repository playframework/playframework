<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Writing functional tests

Play provides a number of classes and convenience methods that assist with functional testing. Most of these can be found either in the [`play.test`](api/java/play/test/package-summary.html) package or in the [`Helpers`](api/java/play/test/Helpers.html) class.

You can add these methods and classes by importing the following:

@[test-imports](code/javaguide/tests/FakeApplicationTest.java)

## Creating `Application` instances for testing

Play frequently requires a running [`Application`](api/java/play/Application.html) as context. To provide an environment for tests, Play provides helpers that produce new application instances for testing:

```java
import static play.test.Helpers.*;
```

@[test-fakeapp](code/javaguide/tests/FakeApplicationTest.java)

## Injecting tests

If you're using Guice for [[dependency injection|JavaDependencyInjection]] then an `Application` for testing can be [[built directly|JavaTestingWithGuice]]. You can also inject any members of a test class that you might need. It's generally best practice to inject members only in functional tests and to manually create instances in unit tests.

@[test-injection](code/javaguide/tests/InjectionTest.java)

## Testing with an application

To run tests with an `Application`, you can do the following:

@[test-running-fakeapp](code/javaguide/tests/FakeApplicationTest.java)

You can also extend [`WithApplication`](api/java/play/test/WithApplication.html), this will automatically ensure that an application is started and stopped for each test method:

@[test-withapp](code/javaguide/tests/FunctionalTest.java)

## Testing with a Guice application

To run tests with an `Application` [[created by Guice|JavaTestingWithGuice]], you can do the following:

@[test-guiceapp](code/tests/guice/JavaGuiceApplicationBuilderTest.java)

Note that there are different ways to customize the `Application` creation when using Guice to test.

## Testing a Controller Action through Routing

With a running application, you can retrieve an action reference from the path for a route and invoke it. This also allows you to use `RequestBuilder` which creates a fake request:

@[bad-route-import](code/javaguide/tests/FunctionalTest.java)

@[bad-route](code/javaguide/tests/FunctionalTest.java)

It is also possible to create the `RequestBuilder` using the reverse router directly and avoid hard-coding the router path:

@[good-route](code/javaguide/tests/FunctionalTest.java)

> **Note:** the reverse router is not executing the action, but instead only providing a `Call` with information that will be used to create the `RequestBuilder` and later invoke the the action itself using `Helpers.route(Application, RequestBuilder)`. That is why it is not necessary to pass a `Http.Request` when using the reverse router to create the `Http.RequestBuilder` in tests even if the action is receiving a `Http.Request` as a parameter.

## Testing with a server

Sometimes you want to test the real HTTP stack from within your test. You can do this by starting a test server:

@[test-server](code/javaguide/tests/FunctionalTest.java)

Just as there exists a `WithApplication` class, there is also a [`WithServer`](api/java/play/test/WithServer.html) which you can extend to automatically start and stop a [`TestServer`](api/java/play/test/TestServer.html) for your tests:

@[test-withserver](code/javaguide/tests/ServerFunctionalTest.java)

## Testing with a browser

If you want to test your application from within a Web browser, you can use [Selenium WebDriver](https://github.com/seleniumhq/selenium). Play will start the WebDriver for you, and wrap it in a [`TestBrowser`](api/java/play/test/TestBrowser.html) backed by [Selenide](https://selenide.org). By default, the browser is [HtmlUnit](https://www.htmlunit.org), which runs in-memory and does not require a browser installation. To test with a real browser, use `FIREFOX`, `CHROME`, `EDGE` or `SAFARI` instead of `HTMLUNIT`. Firefox, Chrome and Edge run headless if the system property `selenide.headless` is `true`, and use the browser executable set by `selenide.browserBinary`. To configure a browser differently, pass your own Selenium `WebDriver` instance.

The test browser needs Selenide, Selenium and HtmlUnit, which you add with the `play-test-browser` dependency:

```scala
libraryDependencies += playTestBrowser % Test
```

With Gradle or Maven, add `org.playframework:play-test-browser_2.13` (or `play-test-browser_3` for Scala 3) as a test dependency.

@[test-browser](code/javaguide/tests/FunctionalTest.java)

`browser.el(selector)` returns the first matching [`SelenideElement`](https://selenide.org/javadoc/current/com/codeborne/selenide/SelenideElement.html) and `browser.$(selector)` (or `browser.find(selector)`) returns all matching elements as a [`BrowserElements`](api/java/play/test/BrowserElements.html) collection, a Selenide [`ElementsCollection`](https://selenide.org/javadoc/current/com/codeborne/selenide/ElementsCollection.html) that can also click, fill and submit all of its elements. Elements are looked up lazily, and actions and assertions on them wait until the element is ready, so you rarely need explicit waits:

@[test-browser-selenide-imports](code/javaguide/tests/FunctionalTest.java)

@[test-browser-selenide](code/javaguide/tests/FunctionalTest.java)

Use `browser.selenide()` to access the complete [Selenide API](https://selenide.org/documentation.html) of the browser and `browser.getDriver()` to access the underlying Selenium `WebDriver`. Settings like timeouts can be changed per browser through `browser.selenideConfig()`, or for all browsers through `selenide.*` system properties or a `selenide.properties` file on the test classpath. Settings used to start a browser, like `selenide.headless` and `selenide.browserBinary`, must be set before the browser is created; changing them through `browser.selenideConfig()` does not affect a running browser.

And, of course there, is the [`WithBrowser`](api/java/play/test/WithBrowser.html) class to automatically open and close a browser for each test:

@[test-withbrowser](code/javaguide/tests/BrowserFunctionalTest.java)
