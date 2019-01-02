<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Writing functional tests with specs2

Play provides a number of classes and convenience methods that assist with functional testing.  Most of these can be found either in the [`play.api.test`](api/scala/play/api/test/index.html) package or in the [`Helpers`](api/scala/play/api/test/Helpers$.html) object.

You can add these methods and classes by importing the following:

@[scalafunctionaltest-imports](code/specs2/ScalaFunctionalTestSpec.scala)

## Creating `Application` instances for testing

Play frequently requires a running [`Application`](api/scala/play/api/Application.html) as context. If you're using the default Guice dependency injection, you can use the [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html) class which can be configured with different configuration, routes, or even additional modules.

@[scalafunctionaltest-application](code/specs2/ScalaFunctionalTestSpec.scala)

## WithApplication

To pass in an application to an example, use [`WithApplication`](api/scala/play/api/test/WithApplication.html).  An explicit [`Application`](api/scala/play/api/Application.html) can be passed in, but a default application (created from the default [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html)) is provided for convenience.

Because [`WithApplication`](api/scala/play/api/test/WithApplication.html) is a built in [`Around`](https://etorreborre.github.io/specs2/guide/SPECS2-3.6.6/org.specs2.guide.Contexts.html#aroundeach) block, you can override it to provide your own data population:

@[scalafunctionaltest-withdbdata](code/specs2/WithDbDataSpec.scala)

## WithServer

Sometimes you want to test the real HTTP stack from within your test, in which case you can start a test server using [`WithServer`](api/scala/play/api/test/WithServer.html):

@[scalafunctionaltest-testpaymentgateway](code/specs2/ScalaFunctionalTestSpec.scala)

The `port` value contains the port number the server is running on.  By default this is 19001, however you can change this either by passing the port into [`WithServer`](api/scala/play/api/test/WithServer.html), or by setting the system property `testserver.port`.  This can be useful for integrating with continuous integration servers, so that ports can be dynamically reserved for each build.

An application can also be passed to the test server, which is useful for setting up custom routes and testing WS calls:

@[scalafunctionaltest-testws](code/specs2/ScalaFunctionalTestSpec.scala)

## WithBrowser

If you want to test your application using a browser, you can use [Selenium WebDriver](https://github.com/seleniumhq/selenium). Play will start the WebDriver for you, and wrap it in the convenient API provided by [FluentLenium](https://github.com/FluentLenium/FluentLenium) using [`WithBrowser`](api/scala/play/api/test/WithBrowser.html).  Like [`WithServer`](api/scala/play/api/test/WithServer.html), you can change the port, [`Application`](api/scala/play/api/Application.html), and you can also select the web browser to use:

@[scalafunctionaltest-testwithbrowser](code/specs2/ScalaFunctionalTestSpec.scala)

## Injecting

There are many functional tests that use the injector directly through the implicit `app`:

@[scalafunctionaltest-noinjecting](code/specs2/ExampleHelpersSpec.scala)

With the [`Injecting`](api/scala/play/api/test/Injecting.html) trait, you can elide this:

@[scalafunctionaltest-injecting](code/specs2/ExampleHelpersSpec.scala)

## PlaySpecification

[`PlaySpecification`](api/scala/play/api/test/PlaySpecification.html) is an extension of [`Specification`](https://etorreborre.github.io/specs2/api/SPECS2-3.6.6/index.html#org.specs2.mutable.Specification) that excludes some of the mixins provided in the default specs2 specification that clash with Play helpers methods.  It also mixes in the Play test helpers and types for convenience.

@[scalafunctionaltest-playspecification](code/specs2/ExamplePlaySpecificationSpec.scala)

## Testing a view template

Since a template is a standard Scala function, you can execute it from your test, and check the result:

@[scalafunctionaltest-testview](code/specs2/ScalaFunctionalTestSpec.scala)

## Testing a controller

You can call any `Action` code by providing a [`FakeRequest`](api/scala/play/api/test/FakeRequest.html):

@[scalafunctionaltest-functionalexamplecontrollerspec](code/specs2/FunctionalExampleControllerSpec.scala)

Technically, you don't need [`WithApplication`](api/scala/play/api/test/WithApplication.html) here, because you can instantiate the controller directly -- however, direct controller instantiation is more of a unit test of the controller than a functional test.

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[scalafunctionaltest-respondtoroute](code/specs2/ScalaFunctionalTestSpec.scala)

## Testing a model

If you are using an SQL database, you can replace the database connection with an in-memory instance of an H2 database using `inMemoryDatabase`.

@[scalafunctionaltest-testmodel](code/specs2/ScalaFunctionalTestSpec.scala)

## Testing Messages API

For functional tests that involve configuration, the best option is to use [`WithApplication`](api/scala/play/api/test/WithApplication.html) and pull in an injected [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html):

@[scalafunctionaltest-testmessages](code/specs2/ScalaFunctionalTestSpec.scala)

If you need to customize the configuration, it's better to add configuration values into the [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html) rather than use the [`DefaultMessagesApiProvider`](api/scala/play/api/i18n/DefaultMessagesApiProvider.html) directly.
