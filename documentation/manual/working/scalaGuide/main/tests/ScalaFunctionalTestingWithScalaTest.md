<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Writing functional tests with ScalaTest

Play provides a number of classes and convenience methods that assist with functional testing.  Most of these can be found either in the [`play.api.test`](api/scala/index.html#play.api.test.package) package or in the [`Helpers`](api/scala/index.html#play.api.test.Helpers$) object. The [_ScalaTest + Play_](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.package) integration library builds on this testing support for ScalaTest.

You can access all of Play's built-in test support and _ScalaTest + Play_ with the following imports:

```scala
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
```

## FakeApplication

Play frequently requires a running [`Application`](api/scala/index.html#play.api.Application) as context: it is usually provided from [`play.api.Play.current`](api/scala/index.html#play.api.Play$).

To provide an environment for tests, Play provides a [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) class which can be configured with a different `Global` object, additional configuration, or even additional plugins.

@[scalafunctionaltest-fakeApplication](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

If all or most tests in your test class need a `FakeApplication`, and they can all share the same `FakeApplication`, mix in trait [`OneAppPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneAppPerSuite). You can access the `FakeApplication` from the `app` field. If you need to customize the `FakeApplication`, override `app` as shown in this example:

@[scalafunctionaltest-oneapppersuite](code-scalatestplus-play/oneapppersuite/ExampleSpec.scala)

If you need each test to get its own `FakeApplication`, instead of sharing the same one, use `OneAppPerTest` instead:

@[scalafunctionaltest-oneapppertest](code-scalatestplus-play/oneapppertest/ExampleSpec.scala)

The reason _ScalaTest + Play_ provides both `OneAppPerSuite` and `OneAppPerTest` is to allow you to select the sharing strategy that makes your tests run fastest. If you want application state maintained between successive tests, you'll need to use `OneAppPerSuite`. If each test needs a clean slate, however, you could either use `OneAppPerTest` or use `OneAppPerSuite`, but clear any state at the end of each test. Furthermore, if your test suite will run fastest if multiple test classes share the same application, you can define a master suite that mixes in [`OneAppPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneAppPerSuite) and nested suites that mix in [`ConfiguredApp`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ConfiguredApp), as shown in the example in the [documentation for `ConfiguredApp`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ConfiguredApp). You can use whichever strategy makes your test suite run the fastest.

## Testing with a server

Sometimes you want to test with the real HTTP stack. If all tests in your test class can reuse the same server instance, you can mix in [`OneServerPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneServerPerSuite) (which will also provide a new `FakeApplication` for the suite):

@[scalafunctionaltest-oneserverpersuite](code-scalatestplus-play/oneserverpersuite/ExampleSpec.scala)

If all tests in your test class require separate server instance, use [`OneServerPerTest`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneServerPerTest) instead (which will also provide a new `FakeApplication` for the suite):

@[scalafunctionaltest-oneserverpertest](code-scalatestplus-play/oneserverpertest/ExampleSpec.scala)

The `OneServerPerSuite` and `OneServerPerTest` traits provide the port number on which the server is running as the `port` field.  By default this is 19001, however you can change this either overriding `port` or by setting the system property `testserver.port`.  This can be useful for integrating with continuous integration servers, so that ports can be dynamically reserved for each build.

You can also customize the [`FakeApplication`](api/scala/index.html#play.api.test.FakeApplication) by overriding `app`, as demonstrated in the previous examples.

Lastly, if allowing multiple test classes to share the same server will give you better performance than either the `OneServerPerSuite` or `OneServerPerTest` approaches, you can define a master suite that mixes in [`OneServerPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneServerPerSuite) and nested suites that mix in [`ConfiguredServer`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ConfiguredServer), as shown in the example in the [documentation for `ConfiguredServer`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ConfiguredServer).

## Testing with a web browser

The _ScalaTest + Play_ library builds on ScalaTest's [Selenium DSL](http://doc.scalatest.org/2.1.5/index.html#org.scalatest.selenium.WebBrowser) to make it easy to test your Play applications from web browsers.

To run all tests in your test class using a same browser instance, mix [`OneBrowserPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneBrowserPerSuite) into your test class. You'll also need to mix in a [`BrowserFactory`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.BrowserFactory) trait that will provide a Selenium web driver: one of [`ChromeFactory`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ChromeFactory), [`FirefoxFactory`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.FirefoxFactory), [`HtmlUnitFactory`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.HtmlUnitFactory), [`InternetExplorerFactory`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.InternetExplorerFactory), [`SafariFactory`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.SafariFactory).

In addition to mixing in a [`BrowserFactory`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.BrowserFactory), you will need to mix in a [`ServerProvider`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ServerProvider) trait that provides a `TestServer`: one of [`OneServerPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneServerPerSuite), [`OneServerPerTest`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneServerPerTest), or [`ConfiguredServer`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ConfiguredServer).

For example, the following test class mixes in `OneServerPerSuite` and `HtmUnitFactory`:

@[scalafunctionaltest-onebrowserpersuite](code-scalatestplus-play/onebrowserpersuite/ExampleSpec.scala)

If each of your tests requires a new browser instance, use [`OneBrowserPerTest`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneBrowserPerSuite) instead. As with `OneBrowserPerSuite`, you'll need to also mix in a `ServerProvider` and `BrowserFactory`:

@[scalafunctionaltest-onebrowserpertest](code-scalatestplus-play/onebrowserpertest/ExampleSpec.scala)

If you need multiple test classes to share the same browser instance, mix [`OneBrowserPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.OneBrowserPerSuite) into a master suite and [`ConfiguredBrowser`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ConfiguredBrowser) into multiple nested suites. The nested suites will all share the same web browser. For an example, see the [documentation for trait `ConfiguredBrowser`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.ConfiguredBrowser).

## Running the same tests in multiple browsers

If you want to run tests in multiple web browsers, to ensure your application works correctly in all the browsers you support, you can use traits [`AllBrowsersPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.AllBrowsersPerSuite) or [`AllBrowsersPerTest`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.AllBrowsersPerTest). Both of these traits declare a `browsers` field of type `IndexedSeq[BrowserInfo]` and an abstract `sharedTests` method that takes a `BrowserInfo`. The `browsers` field indicates which browsers you want your tests to run in. The default is Chrome, Firefox, Internet Explorer, `HtmlUnit`, and Safari. You can override `browsers` if the default doesn't fit your needs. You place tests you want run in multiple browsers in the `sharedTests` method, placing the name of the browser at the end of each test name. (The browser name is available from the `BrowserInfo` passed into `sharedTests`.) Here's an example that uses `AllBrowsersPerSuite`:

@[scalafunctionaltest-allbrowserspersuite](code-scalatestplus-play/allbrowserspersuite/ExampleSpec.scala)

All tests declared by `sharedTests` will be run with all browsers mentioned in the `browsers` field, so long as they are available on the host system. Tests for any browser that is not available on the host system will be canceled automatically. Note that you need to append the `browser.name` manually to the test name to ensure each test in the suite has a unique name (which is required by ScalaTest). If you leave that off, you'll get a duplicate-test-name error when you run your tests.

[`AllBrowsersPerSuite`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.AllBrowsersPerSuite) will create a single instance of each type of browser and use that for all the tests declared in `sharedTests`. If you want each test to have its own, brand new browser instance, use [`AllBrowsersPerTest`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.AllBrowsersPerTest) instead:

@[scalafunctionaltest-allbrowserspertest](code-scalatestplus-play/allbrowserspertest/ExampleSpec.scala)

Although both `AllBrowsersPerSuite` and `AllBrowsersPerTest` will cancel tests for unavailable browser types, the tests will show up as canceled in the output.  To can clean up the output, you can exclude web browsers that will never be available by overriding `browsers`, as shown in this example:

@[scalafunctionaltest-allbrowserspersuite](code-scalatestplus-play/allbrowserspersuite/ExampleOverrideBrowsersSpec.scala)

The previous test class will only attempt to run the shared tests with Firefox and Chrome (and cancel tests automatically if a browser is not available).

## PlaySpec

`PlaySpec` provides a convenience "super Suite" ScalaTest base class for Play tests, You get `WordSpec`, `MustMatchers`, `OptionValues`, and `WsScalaTestClient` automatically by extending `PlaySpec`:

@[scalafunctionaltest-playspec](code-scalatestplus-play/playspec/ExampleSpec.scala)

You can mix any of the previously mentioned traits into `PlaySpec`.

## When different tests need different fixtures

In all the test classes shown in previous examples, all or most tests in the test class required the same fixtures. While this is common, it is not always the case. If different tests in the same test class need different fixtures, mix in trait [`MixedFixtures`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures). Then give each individual test the fixture it needs using one of these no-arg functions: [App](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures$App), [Server](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures$Server), [Chrome](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures$Chrome), [Firefox](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures$Firefox), [HtmlUnit](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures$HtmlUnit), [InternetExplorer](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures$InternetExplorer), or [Safari](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures$Safari).

You cannot mix [`MixedFixtures`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedFixtures) into [`PlaySpec`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.PlaySpec) because `MixedFixtures` requires a ScalaTest [`fixture.Suite`](http://doc.scalatest.org/2.1.5/index.html#org.scalatest.fixture.Suite) and `PlaySpec` is just a regular [`Suite`](http://doc.scalatest.org/2.1.5/index.html#org.scalatest.Suite). If you want a convenient base class for mixed fixtures, extend [`MixedPlaySpec`](http://doc.scalatest.org/plus-play/1.0.0/index.html#org.scalatestplus.play.MixedPlaySpec) instead. Here's an example:

@[scalafunctionaltest-mixedfixtures](code-scalatestplus-play/mixedfixtures/ExampleSpec.scala)

## Testing a template

Since a template is a standard Scala function, you can execute it from your test, and check the result:

@[scalafunctionaltest-testview](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

## Testing a controller

You can call any `Action` code by providing a [`FakeRequest`](api/scala/index.html#play.api.test.FakeRequest):

@[scalatest-examplecontrollerspec](code-scalatestplus-play/ExampleControllerSpec.scala)

Technically, you don't need [`WithApplication`](api/scala/index.html#play.api.test.WithApplication) here, although it wouldn't hurt anything to have it.

## Testing the router

Instead of calling the `Action` yourself, you can let the `Router` do it:

@[scalafunctionaltest-respondtoroute](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

## Testing a model

If you are using an SQL database, you can replace the database connection with an in-memory instance of an H2 database using `inMemoryDatabase`.

@[scalafunctionaltest-testmodel](code-scalatestplus-play/ScalaFunctionalTestSpec.scala)

## Testing WS calls

If you are calling a web service, you can use [`WSTestClient`](api/scala/index.html#play.api.test.WsTestClient).  There are two calls available, `wsCall` and `wsUrl` that will take a Call or a string, respectively.  Note that they expect to be called in the context of `WithApplication`.

```
wsCall(controllers.routes.Application.index()).get()
```

```
wsUrl("http://localhost:9000").get()
```
