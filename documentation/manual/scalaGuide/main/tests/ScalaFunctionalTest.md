# Writing functional tests with ScalaTest

Play provides a number of classes and convenience methods that assist with functional testing.  Most of these can be found either in the [`play.api.test`](api/scala/index.html#play.api.test.package) package or in the [`Helpers`](api/scala/index.html#play.api.test.Helpers$) object. The ScalaTest + Play integration library builds on this testing support for ScalaTest.

You can access all of Play's built-in test support and ScalaTest + Play with the following imports:

```scala
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
```

## FakeApplication

Play frequently requires a running [`Application`](api/scala/index.html#play.api.Application) as context: it is usually provided from [`play.api.Play.current`](api/scala/index.html#play.api.Play$).

To provide an environment for tests, Play provides a [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) class which can be configured with a different Global object, additional configuration, or even additional plugins.

@[scalafunctionaltest-fakeApplication](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

If all the tests in your test class need a `FakeApplication`, and they can all share the same one, mix in trait `OneAppPerSuite`:

@[scalafunctionaltest-oneapppersuite](code-scalatestplus-play/oneapppersuite/ExampleSpec.scala)

If you need each test to get its own `FakeApplication`, use `OneAppPerTest` instead:

@[scalafunctionaltest-oneapppertest](code-scalatestplus-play/oneapppertest/ExampleSpec.scala)

## Testing with Server

Sometimes you want to test the real HTTP stack from your tests. If all tests in your test class can reuse the same server instance, use `OneServerPerSuite` (which will also provide a new `FakeApplication` for the suite):

@[scalafunctionaltest-oneserverpersuite](code-scalatestplus-play/oneserverpersuite/ExampleSpec.scala)

If all tests in your test class requires separate server instance, use `OneServerPerTest` instead (which will also provide a new `FakeApplication` for the suite):

@[scalafunctionaltest-oneserverpertest](code-scalatestplus-play/oneserverpertest/ExampleSpec.scala)

The `port` field contains the port number the server is running on.  By default this is 19001, however you can change this either overriding `port` or by setting the system property `testserver.port`.  This can be useful for integrating with continuous integration servers, so that ports can be dynamically reserved for each build.

You can also customize the [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) by overriding `app`, as demonstrated in the previous examples.

## Testing with Specific Web Browser

ScalaTest + Play provides very good support integration testing using different web browsers.

To run all tests in your test class using a same browser instance, you can use `OneBrowserPerSuite`:

@[scalafunctionaltest-onebrowserpersuite](code-scalatestplus-play/onebrowserpersuite/ExampleSpec.scala)

The example uses `HtmlUnitFactory` that will create `HtmlUnit` web driver, you can use `FirefoxFactory`, `ChromeFactory`, `SafariFactory` or `InternetBrowserFactory`
to create other types of web driver to use.

If each of your test require new browser instance, you can use `OneBrowserPerTest` instead:

@[scalafunctionaltest-onebrowserpertest](code-scalatestplus-play/onebrowserpertest/ExampleSpec.scala)


## Testing with ALL Available Web Browsers

If you want to run your tests with all available web browsers on your system, you can use `AllBrowsersPerSuite`:

@[scalafunctionaltest-allbrowserspersuite](code-scalatestplus-play/allbrowserspersuite/ExampleSpec.scala)

All tests registered under `sharedTests` will be run with all available browsers on your system.  Note that it is important for you to append the `browser.name` manually to the test name, without it you'll get a duplicated test name error at runtime.

Using `AllBrowsersPerSuite` all tests will be run by the same instance for a browser type, if you want a new instance for each test, you can use `AllBrowsersPerTest` instead:

@[scalafunctionaltest-allbrowserspertest](code-scalatestplus-play/allbrowserspertest/ExampleSpec.scala)

For both `AllBrowsersPerSuite` and `AllBrowsersPerTest`, when a browser type is not available on the running system, the test will be canceled automatically and shown in output.  You can explicitly specify web browser(s) to be included by overriding `browsers`:

@[scalafunctionaltest-allbrowserspersuite](code-scalatestplus-play/allbrowserspersuite/ExampleOverrideBrowsersSpec.scala)

`AllBrowsersPerSuite` will then try to detect only Firefox and Chrome browser in the running system (and cancel test automatically if the browser is not available).  The same approach can be used on `AllBrowsersPerTest`.

## PlaySpec

`PlaySpec` provides a convenience "super Suite" ScalaTest base class for Play tests, you get `MustMatchers`, `OptionValues` and `WsScalaTestClient` automatically by extending `PlaySpec`:

@[scalafunctionaltest-playspec](code-scalatestplus-play/playspec/ExampleSpec.scala)

## Testing a template

Since a template is a standard Scala function, you can execute it from your test, and check the result:

@[scalafunctionaltest-testview](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

## Testing a controller

You can call any `Action` code by providing a [`FakeRequest`](api/scala/index.html#play.api.test.FakeRequest):

@[scalatest-examplecontrollerspec](code-scalatestplus-play/ExampleControllerSpec.scala)

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[scalafunctionaltest-respondtoroute](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

## Testing a model

If you are using an SQL database, you can replace the database connection with an in-memory instance of an H2 database using `inMemoryDatabase`.

@[scalafunctionaltest-testmodel](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

> **Next:** [[Advanced topics|Iteratees]]
