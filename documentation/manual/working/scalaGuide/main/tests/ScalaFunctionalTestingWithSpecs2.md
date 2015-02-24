<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Writing functional tests with specs2

Play provides a number of classes and convenience methods that assist with functional testing.  Most of these can be found either in the [`play.api.test`](api/scala/index.html#play.api.test.package) package or in the [`Helpers`](api/scala/index.html#play.api.test.Helpers$) object.

You can add these methods and classes by importing the following:

```scala
import play.api.test._
import play.api.test.Helpers._
```

## FakeApplication

Play frequently requires a running [`Application`](api/scala/index.html#play.api.Application) as context: it is usually provided from [`play.api.Play.current`](api/scala/index.html#play.api.Play$).

To provide an environment for tests, Play provides a [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) class which can be configured with a different Global object, additional configuration, or even additional plugins.

@[scalafunctionaltest-fakeApplication](code/specs2/ScalaFunctionalTestSpec.scala)

## WithApplication

To pass in an application to an example, use [`WithApplication`](api/scala/index.html#play.api.test.WithApplication).  An explicit [`Application`](api/scala/index.html#play.api.Application) can be passed in, but a default [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) is provided for convenience.

Because [`WithApplication`](api/scala/index.html#play.api.test.WithApplication) is a built in [`Around`](http://etorreborre.github.io/specs2/guide/org.specs2.guide.Structure.html#Around) block, you can override it to provide your own data population:

@[scalafunctionaltest-withdbdata](code/specs2/WithDbDataSpec.scala)

## WithServer

Sometimes you want to test the real HTTP stack from within your test, in which case you can start a test server using [`WithServer`](api/scala/index.html#play.api.test.WithServer):

@[scalafunctionaltest-testpaymentgateway](code/specs2/ScalaFunctionalTestSpec.scala)

The `port` value contains the port number the server is running on.  By default this is 19001, however you can change this either by passing the port into [`WithServer`](api/scala/index.html#play.api.test.WithServer), or by setting the system property `testserver.port`.  This can be useful for integrating with continuous integration servers, so that ports can be dynamically reserved for each build.

A [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) can also be passed to the test server, which is useful for setting up custom routes and testing WS calls:

@[scalafunctionaltest-testws](code/specs2/ScalaFunctionalTestSpec.scala)

## WithBrowser

If you want to test your application using a browser, you can use [Selenium WebDriver](http://code.google.com/p/selenium/?redir=1). Play will start the WebDriver for you, and wrap it in the convenient API provided by [FluentLenium](https://github.com/FluentLenium/FluentLenium) using [`WithBrowser`](api/scala/index.html#play.api.test.WithBrowser).  Like [`WithServer`](api/scala/index.html#play.api.test.WithServer), you can change the port, [`Application`](api/scala/index.html#play.api.Application), and you can also select the web browser to use:

@[scalafunctionaltest-testwithbrowser](code/specs2/ScalaFunctionalTestSpec.scala)

## PlaySpecification

[`PlaySpecification`](api/scala/index.html#play.api.test.PlaySpecification) is an extension of [`Specification`](http://etorreborre.github.io/specs2/api/SPECS2-2.4.9/index.html#org.specs2.mutable.Specification) that excludes some of the mixins provided in the default specs2 specification that clash with Play helpers methods.  It also mixes in the Play test helpers and types for convenience.

@[scalafunctionaltest-playspecification](code/specs2/ExamplePlaySpecificationSpec.scala)

## Testing a view template

Since a template is a standard Scala function, you can execute it from your test, and check the result:

@[scalafunctionaltest-testview](code/specs2/ScalaFunctionalTestSpec.scala)

## Testing a controller

You can call any `Action` code by providing a [`FakeRequest`](api/scala/index.html#play.api.test.FakeRequest):

@[scalafunctionaltest-functionalexamplecontrollerspec](code/specs2/FunctionalExampleControllerSpec.scala)

Technically, you don't need [`WithApplication`](api/scala/index.html#play.api.test.WithApplication) here, although it wouldn't hurt anything to have it.

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[scalafunctionaltest-respondtoroute](code/specs2/ScalaFunctionalTestSpec.scala)

## Testing a model

If you are using an SQL database, you can replace the database connection with an in-memory instance of an H2 database using `inMemoryDatabase`.

@[scalafunctionaltest-testmodel](code/specs2/ScalaFunctionalTestSpec.scala)
