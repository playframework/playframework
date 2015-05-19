<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Writing functional tests

Play provides a number of classes and convenience methods that assist with functional testing. Most of these can be found either in the [`play.test`](api/java/play/test/package-summary.html) package or in the [`Helpers`](api/java/play/test/Helpers.html) class.

You can add these methods and classes by importing the following:

```java
import play.test.*;
import static play.test.Helpers.*;
```

## FakeApplication

Play frequently requires a running [`Application`](api/java/play/Application.html) as context: it is usually provided from [`play.Play.application()`](api/java/play/Play.html).

To provide an environment for tests, Play provides a [`FakeApplication`](api/java/play/test/FakeApplication.html) class which can be configured with a different Global object, additional configuration, or even additional plugins.

@[test-fakeapp](code/javaguide/tests/FakeApplicationTest.java)

## Injecting tests

If you're using Guice for [[dependency injection|JavaDependencyInjection]] then an `Application` for testing can be [[built directly|JavaTestingWithGuice]], instead of using FakeApplication. You can also inject any members of a test class that you might need. It's generally best practice to inject members only in functional tests and to manually create instances in unit tests.

@[test-injection](code/javaguide/tests/InjectionTest.java)

## Testing with an application

To run tests with an `Application`, you can do the following:

@[test-running-fakeapp](code/javaguide/tests/FakeApplicationTest.java)

You can also extend [`WithApplication`](api/java/play/test/WithApplication.html), this will automatically ensure that an application is started and stopped for you:

@[test-withapp](code/javaguide/tests/FunctionalTest.java)

This will ensure that a [`FakeApplication`](api/java/play/test/FakeApplication.html) will be started and stopped for each test method.

## Testing with a Guice application

To run tests with an `Application` [[created by Guice|JavaTestingWithGuice]], you can do the following:

@[test-guiceapp](code/tests/guice/JavaGuiceApplicationBuilderTest.java)

Note that there are different ways to customize the `Application` creation when using Guice to test.

## Testing with a server

Sometimes you want to test the real HTTP stack from within your test. You can do this by starting a test server:

@[test-server](code/javaguide/tests/FunctionalTest.java)

Just as there exists a `WithApplication` class, there is also a [`WithServer`](api/java/play/test/WithServer.html) which you can extend to automatically start and stop a [`TestServer`](api/java/play/test/TestServer.html) for your tests:

@[test-withserver](code/javaguide/tests/ServerFunctionalTest.java)

## Testing with a browser

If you want to test your application from with a Web browser, you can use [Selenium WebDriver](https://github.com/seleniumhq/selenium). Play will start the WebDriver for you, and wrap it in the convenient API provided by [FluentLenium](https://github.com/FluentLenium/FluentLenium).

@[test-browser](code/javaguide/tests/FunctionalTest.java)

And, of course there, is the [`WithBrowser`](api/java/play/test/WithBrowser.html) class to automatically open and close a browser for each test:

@[test-withbrowser](code/javaguide/tests/BrowserFunctionalTest.java)

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[bad-route-import](code/javaguide/tests/FunctionalTest.java)

@[bad-route](code/javaguide/tests/FunctionalTest.java)
