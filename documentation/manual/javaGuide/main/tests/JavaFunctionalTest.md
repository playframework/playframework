<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
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

## Testing with a fake application

To run tests within a `FakeAppliction`, you can do the following:

Java
: @[test-running-fakeapp](code/javaguide/tests/FakeApplicationTest.java)

Java 8
: @[test-running-fakeapp](java8code/java8guide/tests/FakeApplicationTest.java)

You can also extend `WithApplication`, this will automatically ensure that a fake application is started and stopped for you:

@[test-withapp](code/javaguide/tests/FunctionalTest.java)

## Testing with a server

Sometimes you want to test the real HTTP stack from with your test. You can do this by starting a test server:

Java
: @[test-server](code/javaguide/tests/FunctionalTest.java)

Java 8
: @[test-server](java8code/java8guide/tests/FunctionalTest.java)

## Testing with a browser

If you want to test your application from with a Web browser, you can use [Selenium WebDriver](http://code.google.com/p/selenium/?redir=1). Play will start the WebDriver for you, and wrap it in the convenient API provided by [FluentLenium](https://github.com/FluentLenium/FluentLenium).

Java
: @[with-browser](code/javaguide/tests/FunctionalTest.java)

Java 8
: @[with-browser](java8code/java8guide/tests/FunctionalTest.java)

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[bad-route](code/javaguide/tests/FunctionalTest.java)

> **Next:** [[Logging|JavaLogging]]