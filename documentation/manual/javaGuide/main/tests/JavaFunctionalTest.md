<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Writing functional tests

## Testing a template

As a template is a standard Scala function, you can execute it from a test and check the result:

@[render](code/javaguide/tests/JavaFunctionalTest.java)

You can find the complete list of the *test helpers* in the [Helper class API documentation](http://www.playframework.com/documentation/api/2.1.1/java/play/test/Helpers.html). 

## Testing your controllers

You can also retrieve an action reference from the reverse router, such as `controllers.routes.ref.Application.index`. You can then invoke it:

@[action-ref](code/javaguide/tests/JavaFunctionalTest.java)

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[bad-route](code/javaguide/tests/JavaFunctionalTest.java)

## Starting a real HTTP server

Sometimes you want to test the real HTTP stack from with your test. You can do this by starting a test server:

Java
: @[test-server](code/javaguide/tests/JavaFunctionalTest.java)

Java 8
: @[test-server](java8code/java8guide/tests/JavaFunctionalTest.java)

## Testing from within a web browser

If you want to test your application from with a Web browser, you can use [Selenium WebDriver](http://code.google.com/p/selenium/?redir=1). Play will start the WebDriver for you, and wrap it in the convenient API provided by [FluentLenium](https://github.com/FluentLenium/FluentLenium).

Java
: @[with-browser](code/javaguide/tests/JavaFunctionalTest.java)

Java 8
: @[with-browser](java8code/java8guide/tests/JavaFunctionalTest.java)
