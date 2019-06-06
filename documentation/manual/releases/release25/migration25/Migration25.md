<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.5 Migration Guide

This is a guide for migrating from Play 2.4 to Play 2.5. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.4 Migration Guide|Migration24]].

As well as the information contained on this page, there is more detailed migration information for some topics:

- [[Streams Migration Guide|StreamsMigration25]] – Migrating to Akka Streams, now used in place of iteratees in many Play APIs
- [[Java Migration Guide|JavaMigration25]] - Migrating Java applications. Play now uses native Java types for functional types and offers several new customizable components in Java.

Lucidchart has also put together an informative blog post on [upgrading from Play 2.3.x to Play 2.5.x](https://www.lucidchart.com/techblog/2017/02/22/upgrading-play-framework-2-3-play-2-5/).

## How to migrate

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

### Play upgrade

Update the Play version number in project/plugins.sbt to upgrade Play:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.x")
```

Where the "x" in `2.5.x` is the minor version of Play you want to use, per instance `2.5.0`.

### sbt upgrade to 0.13.11

Although Play 2.5 will still work with sbt 0.13.8, we recommend upgrading to the latest sbt version, 0.13.11.  The 0.13.11 release of sbt has a number of [improvements and bug fixes](https://github.com/sbt/sbt/releases/tag/v0.13.11).

Update your `project/build.properties` so that it reads:

```
sbt.version=0.13.11
```

### Play Slick upgrade

If your project is using Play Slick, you need to upgrade it:

```scala
libraryDependencies += "com.typesafe.play" %% "play-slick" % "2.0.0"
```

Or:

```scala
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0"
)
```

### Play Ebean upgrade

If your project is using Play Ebean, you need to upgrade it:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "3.0.0")
```

### ScalaTest + Plus upgrade

If your project is using [[ScalaTest + Play|ScalaTestingWithScalaTest]], you need to upgrade it:

```scala
libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test"
)
```

## Scala 2.10 support discontinued

Play 2.3 and 2.4 supported both Scala 2.10 and 2.11. Play 2.5 has dropped support for Scala 2.10 and now only supports Scala 2.11. There are a couple of reasons for this:

1. Play 2.5's internal code makes extensive use of the [scala-java8-compat](https://github.com/scala/scala-java8-compat) library, which only supports Scala 2.11. The *scala-java8-compat* has conversions between many Scala and Java 8 types, such as Scala `Future`s and Java `CompletionStage`s. (You might find this library useful for your code too.)

2. The next version of Play will probably add support for Scala 2.12. It's time for Play to move to Scala 2.11 so that the upcoming transition to 2.12 will be easier.

### How to migrate

**Both Scala and Java users** must configure sbt to use Scala 2.11.  Even if you have no Scala code in your project, Play itself uses Scala and must be configured to use the right Scala libraries.

To set the Scala version in sbt, simply set the `scalaVersion` key, eg:

```scala
scalaVersion := "2.11.8"
```

If you have a single project build, then this setting can just be placed on its own line in `build.sbt`.  However, if you have a multi project build, then the scala version setting must be set on each project.  Typically, in a multi project build, you will have some common settings shared by every project, this is the best place to put the setting, eg:

```scala
def common = Seq(
  scalaVersion := "2.11.8"
)

lazy val projectA = (project in file("projectA"))
  .enablePlugins(PlayJava)
  .settings(common: _*)

lazy val projectB = (project in file("projectB"))
  .enablePlugins(PlayJava)
  .settings(common: _*)
```

## Change to Logback configuration

As part of the change to remove Play's hardcoded dependency on Logback [[(see Highlights)|Highlights25#Support-for-other-logging-frameworks]], one of the classes used by Logback configuration had to be moved to another package.

### How to migrate

You will need to update your Logback configuration files (`logback*.xml`) and change any references to the old `play.api.Logger$ColoredLevel` to the new `play.api.libs.logback.ColoredLevel` class.

The new configuration after the change will look something like this:

```xml
<conversionRule conversionWord="coloredLevel"
  converterClass="play.api.libs.logback.ColoredLevel" />
```

If you use compile time dependency injection, you will need to change your application loader from using `Logger.configure(...)` to the following:

```scala
LoggerConfigurator(context.environment.classLoader).foreach { _.configure(context.environment) }
```

You can find more details on how to set up Play with different logging frameworks are in [[Configuring logging|SettingsLogger#Using-a-Custom-Logging-Framework]] section of the documentation.

## Play WS upgrades to AsyncHttpClient 2

Play WS has been upgraded to use [AsyncHttpClient 2](https://github.com/AsyncHttpClient/async-http-client).  This is a major upgrade that uses Netty 4.0. Most of the changes in AHC 2.0 are under the hood, but AHC has some significant refactorings which require breaking changes to the WS API:

* `AsyncHttpClientConfig` replaced by [`DefaultAsyncHttpClientConfig`](https://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/DefaultAsyncHttpClientConfig.html).
* [`allowPoolingConnection`](https://static.javadoc.io/com.ning/async-http-client/1.9.32/com/ning/http/client/AsyncHttpClientConfig.html#allowPoolingConnections) and `allowSslConnectionPool` are combined in AsyncHttpClient into a single `keepAlive` variable.  As such, `play.ws.ning.allowPoolingConnection` and `play.ws.ning.allowSslConnectionPool` are not valid and will throw an exception if configured.
* [`webSocketIdleTimeout`](https://static.javadoc.io/com.ning/async-http-client/1.9.32/com/ning/http/client/AsyncHttpClientConfig.html#webSocketTimeout) has been removed, so is no longer available in `AhcWSClientConfig`.
* [`ioThreadMultiplier`](https://static.javadoc.io/com.ning/async-http-client/1.9.32/com/ning/http/client/AsyncHttpClientConfig.html#ioThreadMultiplier) has been removed, so is no longer available in `AhcWSClientConfig`.
* [`FluentCaseInsensitiveStringsMap`](https://static.javadoc.io/com.ning/async-http-client/1.9.32/com/ning/http/client/FluentCaseInsensitiveStringsMap.html) class is removed and replaced by Netty's `HttpHeader` class.
* [`Realm.AuthScheme.None`](https://static.javadoc.io/com.ning/async-http-client/1.9.32/com/ning/http/client/Realm.AuthScheme.html#NONE) has been removed, so is no longer available in `WSAuthScheme`.

In addition, there are number of small changes:

* In order to reflect the proper AsyncHttpClient library name, package `play.api.libs.ws.ning` was renamed into `play.api.libs.ws.ahc` and `Ning*` classes were renamed into `Ahc*`.  In addition, the AHC configuration settings have been changed to `play.ws.ahc` prefix, i.e. `play.ws.ning.maxConnectionsPerHost` is now `play.ws.ahc.maxConnectionsPerHost`.
* The deprecated interface `play.libs.ws.WSRequestHolder` has been removed.
* The `play.libs.ws.play.WSRequest` interface now returns `java.util.concurrent.CompletionStage` instead of `F.Promise`.
* Static methods that rely on `Play.current` or `Play.application` have been deprecated.
* Play WS would infer a charset from the content type and append a charset to the `Content-Type` header of the request if one was not already set.  This caused some confusion and bugs, and so in 2.5.x the `Content-Type` header does not automatically include an inferred charset.  If you explicitly set a `Content-Type` header, the setting is honored as is.

## Deprecated `GlobalSettings`

As part of the on going efforts to move away from global state in Play, `GlobalSettings` and the application `Global` object have been deprecated.  For more details, see the [[Play 2.4 migration guide|GlobalSettings]] for how to migrate away from using `GlobalSettings`.

## Removed Plugins API

The Plugins API was deprecated in Play 2.4 and has been removed in Play 2.5. The Plugins API has been superseded by Play's dependency injection and module system which provides a cleaner and more flexible way to build reusable components.  For details on how to migrate from plugins to dependency injection see the [[Play 2.4 migration guide|PluginsToModules]].

## Routes generated with InjectedRoutesGenerator

Routes are now generated using the dependency injection aware `InjectedRoutesGenerator`, rather than the previous `StaticRoutesGenerator` which assumed controllers were singleton objects.

To revert back to the earlier behavior (if you have "object MyController" in your code, for example), please add the following line to your `build.sbt` file:

```scala
routesGenerator := StaticRoutesGenerator
```

If you're using `Build.scala` instead of `build.sbt` you will need to import the `routesGenerator` settings key:

````scala
import play.sbt.routes.RoutesCompiler.autoImport._
````

Using static controllers with the static routes generator is not deprecated, but it is recommended that you migrate to using classes with dependency injection.

## Replaced static controllers with dependency injection

`controllers.ExternalAssets` is now a class, and has no static equivalent. `controllers.Assets` and `controllers.Default` are also classes, and while static equivalents exist, it is recommended that you use the class version.

### How to migrate

The recommended solution is to use classes for all your controllers. The `InjectedRoutesGenerator` is now the default, so the controllers in the routes file are assumed to be classes instead of objects.

If you still have static controllers, you can use `StaticRoutesGenerator` (described above) and add the `@` symbol in front of the route in the `routes` file, e.g.

```
GET  /assets/*file  @controllers.ExternalAssets.at(path = "/public", file)
```

## Deprecated play.Play and play.api.Play methods

The following methods have been deprecated in `play.Play`:

* `public static Application application()`
* `public static Mode mode()`
* `public static boolean isDev()`
* `public static boolean isProd()`
* `public static boolean isTest()`

Likewise, methods in `play.api.Play` that take an implicit `Application` and delegate to Application, such as `def classloader(implicit app: Application)` are now deprecated.

### How to migrate

These methods delegate to either `play.Application` or `play.Environment` -- code that uses them should use dependency injection to inject the relevant class.

You should refer to the list of dependency injected components in the [[Play 2.4 Migration Guide|Migration24#Dependency-Injected-Components]] to migrate built-in Play components.

For example, the following code injects an environment and configuration into a Controller in Scala:

```scala
class HomeController @Inject() (environment: play.api.Environment,
    configuration: play.api.Configuration)
  extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def config = Action {
    Ok(configuration.underlying.getString("some.config"))
  }

  def count = Action {
    val num = environment.resource("application.conf").toSeq.size
    Ok(num.toString)
  }
}
```

### Handling legacy components

Generally the components you use should not need to depend on the entire application, but sometimes you have to deal with legacy components that require one. You can handle this by injecting the application into one of your components:

```scala
class FooController @Inject() (appProvider: Provider[Application])
  extends Controller {
  implicit lazy val app = appProvider.get()
  def bar = Action {
    Ok(Foo.bar(app))
  }
}
```

Note that you usually want to use a `Provider[Application]` in this case to avoid circular dependencies.

Even better, you can make your own `*Api` class that turns the static methods into instance methods:

```scala
class FooApi @Inject() (appProvider: Provider[Application]) {
  implicit lazy val app = appProvider.get()
  def bar = Foo.bar(app)
  def baz = Foo.baz(app)
}
```

This allows you to benefit from the testability you get with DI and still use your library that uses global state.

## Content-Type charset changes

Prior to Play 2.5, Play would add a `charset` parameter to certain content types that do not define a charset parameter, specifically [`application/json`](https://www.iana.org/assignments/media-types/application/json) and [`application/x-www-form-urlencoded`](https://www.iana.org/assignments/media-types/application/x-www-form-urlencoded). Now the `Content-Type` is sent without a charset by default. This applies both to sending requests with `WS` and returning responses from Play actions. If you have a non-spec-compliant client or server that requires you to send a charset parameter, you can explicitly set the `Content-Type` header.

## Guice injector and Guice builder changes

By default, Guice can resolve your circular dependency by proxying an interface in the cycle. Since circular dependencies are generally a code smell, and you can also inject Providers to break the cycle, we have chosen to disable this feature on the default Guice injector. Other DI frameworks also are not likely to have this feature, so it can lead to problems when writing Play modules.

Now there are four new methods on the Guice builders (`GuiceInjectorBuilder` and `GuiceApplicationBuilder`) for customizing how Guice injects your classes:
* `disableCircularProxies`: disables the above-mentioned behaviour of proxying interfaces to resolve circular dependencies. To allow proxying use `disableCircularProxies(false)`.
* `requireExplicitBindings`: instructs the injector to only inject classes that are explicitly bound in a module. Can be useful in testing for verifying bindings.
* `requireAtInjectOnConstructors`: requires a constructor annotated with @Inject to instantiate a class.
* `requireExactBindingAnnotations`: disables the error-prone feature in Guice where it can substitute a binding for @Named Foo when injecting @Named("foo") Foo.

## CSRF changes

In order to make Play's CSRF filter more resilient to browser plugin vulnerabilities and new extensions, the default configuration for the CSRF filter has been made far more conservative.  The changes include:

* Instead of blacklisting `POST` requests, now only `GET`, `HEAD` and `OPTIONS` requests are whitelisted, and all other requests require a CSRF check.  This means `DELETE` and `PUT` requests are now checked.
* Instead of blacklisting `application/x-www-form-urlencoded`, `multipart/form-data` and `text/plain` requests, requests of all content types, including no content type, require a CSRF check.  One consequence of this is that AJAX requests that use `application/json` now need to include a valid CSRF token in the `Csrf-Token` header.
* Stateless header-based bypasses, such as the `X-Requested-With`, are disabled by default.

There's a new config option to bypass the new CSRF protection for requests with certain headers. This config option is turned on by default for the Cookie and Authorization headers, so that REST clients, which typically don't use session authentication, will still work without having to send a CSRF token.

However, since the config option allows through *all* requests without those headers, applications that use other authentication schemes (NTLM, TLS client certificates) will be vulnerable to CSRF. These applications should disable the config option so that their authenticated (cookieless) requests are protected by the CSRF filter.

Finally, an additional option has been added to disable the CSRF check for origins trusted by the CORS filter. Please note that the CORS filter must come *before* the CSRF filter in your filter chain for this to work!

Play's old default behaviour can be restored by adding the following configuration to `application.conf`:

```
play.filters.csrf {
  header {
    bypassHeaders {
      X-Requested-With = "*"
      Csrf-Token = "nocheck"
    }
    protectHeaders = null
  }
  bypassCorsTrustedOrigins = false
  method {
    whiteList = []
    blackList = ["POST"]
  }
  contentType.blackList = ["application/x-www-form-urlencoded", "multipart/form-data", "text/plain"]
}
```

### Getting the CSRF token

Previously, a CSRF token could be retrieved from the HTTP request in any action. Now you must have either a CSRF filter or a CSRF action for `CSRF.getToken` to work. If you're not using a filter, you can use the `CSRFAddToken` action in Scala or `AddCSRFToken` Java annotation to ensure a token is in the session.

Also, a minor bug was fixed in this release in which the CSRF token would be empty (throwing an exception in the template helper) if its signature was invalid. Now it will be regenerated on the same request so a token is still available from the template helpers and `CSRF.getToken`.

For more details, please read the CSRF documentation for [[Java|JavaCsrf]] and [[Scala|ScalaCsrf]].

## Crypto Deprecated

From Play 1.x, Play has come with a `Crypto` object that provides some cryptographic operations.  This used internally by Play.  The `Crypto` object is not mentioned in the documentation, but is mentioned as “cryptographic utilities” in the scaladoc.

For a variety of reasons, providing cryptographic utilities as a convenience has turned out not to be workable.   In 2.5.x, the Play-specific functionality has been broken into `CookieSigner`, `CSRFTokenSigner` and `AESSigner` traits, and the `Crypto` singleton object deprecated.

### How to Migrate

Cryptographic migration will depend on your use case, especially if there is unsafe construction of the cryptographic primitives.  The short version is to use [Kalium](https://abstractj.github.io/kalium/) if possible, otherwise use [KeyCzar](https://github.com/google/keyczar) or straight [JCA](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html).

Please see [[Crypto Migration|CryptoMigration25]] for more details.

## Netty 4 upgrade

Netty has been upgraded from 3.10 to 4.0.  One consequence of this is the configuration options for configuring Netty channel options have changed.  The full list options can be seen [here](https://netty.io/4.0/api/io/netty/channel/ChannelOption.html).

### How to Migrate

Modify any `play.server.netty.option` keys to use the new keys defined in [ChannelOption](https://netty.io/4.0/api/io/netty/channel/ChannelOption.html).  A mapping of some of the more popularly used ones is:

| **Old** | **New** |
| ------------------
| `play.server.netty.option.backlog` | `play.server.netty.option.SO_BACKLOG` |
| `play.server.netty.option.child.keepAlive` | `play.server.netty.option.child.SO_KEEPALIVE` |
| `play.server.netty.option.child.tcpNoDelay` | `play.server.netty.option.child.TCP_NODELAY` |

## Changes to `sendFile`, `sendPath` and `sendResource` methods

Java (`play.mvc.StatusHeader`) and Scala (`play.api.mvc.Results.Status`) APIs had the following behavior before:

| API   | Method                                     | Default      |
|:------|:-------------------------------------------|:-------------|
| Scala | `play.api.mvc.Results.Status.sendResource` | `inline`     |
| Scala | `play.api.mvc.Results.Status.sendPath`     | `attachment` |
| Scala | `play.api.mvc.Results.Status.sendFile`     | `attachment` |
| Java  | `play.mvc.StatusHeader.sendInputStream`    | `none`       |
| Java  | `play.mvc.StatusHeader.sendResource`       | `inline`     |
| Java  | `play.mvc.StatusHeader.sendPath`           | `attachment` |
| Java  | `play.mvc.StatusHeader.sendFile`           | `inline`     |

In other words, they were mixing `inline` and `attachment` modes when delivering files. Now, when delivering files, paths and resources uses `inline` as the default behavior. Of course, you can alternate between these two modes using the parameters present in these methods.
