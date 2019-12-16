<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Play 2.8 Migration Guide

This guide is for migrating from Play 2.7 to Play 2.8. See the [[Play 2.7 Migration Guide|Migration27]] to upgrade from Play 2.6. It is also recommended to read [Akka 2.5 to 2.6 migration guide](https://doc.akka.io/docs/akka/2.6.0-M5/project/migration-guide-2.5.x-2.6.x.html) since multiple changes there have an impact on Play 2.8.

## How to migrate

Before starting `sbt`, make sure to make the following upgrades.

### Play update

Update the Play version number in `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.x")
```

Where the "x" in `2.8.x` is the minor version of Play you want to use, for instance `2.8.0`.

### sbt upgrade

Although Play 2.8 still supports sbt 0.13, we recommend that you use sbt 1. This new version is supported and actively maintained. To update, change your `project/build.properties` so that it reads:

```properties
sbt.version=1.3.5
```

At the time of this writing `1.3.5` is the latest version in the sbt 1.x family, you may be able to use newer versions too. Check the release notes for both Play's minor version releases and sbt's [releases](https://github.com/sbt/sbt/releases) for details.

## API Changes

Play 2.8 contains multiple API changes. As usual, we follow our policy of deprecating existing APIs before removing them. This section details these changes.

### SSLEngineProvider makes SSLContext visible

When [[configuring HTTPS|ConfiguringHttps]], it is possible to override the [`play.server.SSLEngineProvider`](api/java/play/server/SSLEngineProvider.html) if you need to fully configure the SSL certificates. Before Play 2.8.0, the provider only made the [SSLEngine](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLEngine.html) visible. Now, to better integrate with third-party libraries, the [SSLContext](https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLContext.html) must be visible as well. See more details at [[ConfiguringHttps]] page.

### Scala 2.11 support discontinued

Play 2.8 support Scala 2.12 and 2.13, but not 2.11, which has reached its end of life.

### Setting `scalaVersion` in your project

**Both Scala and Java users** must configure sbt to use Scala 2.12 or 2.13.  Even if you have no Scala code in your project, Play itself uses Scala and must be configured to use the right Scala libraries.

To set the Scala version in sbt, simply set the `scalaVersion` key, for example:

```scala
scalaVersion := "2.13.1"
```

If you have a single project build, then this setting can just be placed on its own line in `build.sbt`.  However, if you have a multi-project build, then the scala version setting must be set on each project.  Typically, in a multi-project build, you will have some common settings shared by every project, this is the best place to put the setting, for example:

```scala
def commonSettings = Seq(
  scalaVersion := "2.13.1"
)

val projectA = (project in file("projectA"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)

val projectB = (project in file("projectB"))
  .enablePlugins(PlayJava)
  .settings(commonSettings)
```

### File serving methods changed the type of their `filename` parameters

Methods for serving files, like `ok(File content, ...)` (and similar) in the [Java API](api/java/play/mvc/Results.html#ok-java.io.File-) or `sendFile`, `sendPath` and `sendResource` in both Java's  [`StatusHeader`](api/java/play/mvc/StatusHeader.html) and Scala's [`Status`](api/scala/play/api/mvc/Results$Status.html) class changed the type of their `filename` parameters: Instead of using a plain `String`, the Scala API now uses an `Option[String]` as return type for its `filename` parameter function. The Java API changed the parameter type to be an `Optional<String>`.
This API change better reflects the fact that you can pass `None` / `Optional.empty()` if you don't want the `Content-Disposition` header to include a filename.

### The `Headers` class of the Java API is immutable

The class [`play.mvc.Http.Headers`](api/java/play/mvc/Http.Headers.html) is immutable now. Some methods have been deprecated and were replaced with new ones:

| **Deprecated method**                         | **New method**
| ----------------------------------------------|-------------------------------------------
| `toMap()`                                     | `asMap()`
| `addHeader(String name, String value)`        | `adding(String name, String value)`
| `addHeader(String name, List<String> values)` | `adding(String name, List<String> values)`
| `remove(String name)`                         | `removing(String name)`

Be aware that the old deprecated methods return the original, modified `Headers` object, whereas the new methods always return a new object, leaving the original object unmodified.

### Deprecated APIs were removed

Many APIs that were deprecated in earlier versions were removed in Play 2.8. If you are still using them we recommend migrating to the new APIs before upgrading to Play 2.8. Check the Javadocs and Scaladocs for migration notes. See also the [[migration guide for Play 2.7|Migration27]] for more information.

#### Java API

In Play 2.7 we deprecate `play.mvc.Http.Context` in favor of directly using `play.mvc.Http.RequestHeader` or `play.mvc.Http.Request`. We have now removed `Http.Context` and if your application depends on it, you should read [[Play 2.7 migration guide instructions|JavaHttpContextMigration27]].

### Cache Api changes

* `Caffeine` implementations of the `getOrElseUpdate` methods in both `SyncCacheApi` and `AsyncCacheApi` are now atomic. (Note that EhCache implementations of `getOrElseUpdate` methods are still non-atomic)
* `Caffeine` implementations of the `getOrElseUpdate` methods lazily evaluate the `orElse` part, that means if the item with the given key exists in cache then the `orElse` part is not executed
* `play.cache.caffeine.CaffeineDefaultExpiry` class is now deprecated

## Configuration changes

This section lists changes and deprecations in configurations.

### `ObjectMapper` serialization change

Play 2.8 adopts Akka Jackson Serialization support and then uses the defaults provided by Akka. One of the changes is how [Java Time](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html) types are rendered. Until Play 2.7 they were rendered as timestamps, which has better performance, but now they are rendered using [ISO-8601](https://www.iso.org/iso-8601-date-and-time-format.html) ([rfc3339](https://tools.ietf.org/html/rfc3339)) format (`yyyy-MM-dd'T'HH:mm:ss.SSSZ`).

If you need to use the old timestamps default format, then add the following configuration in your `application.conf`:

```HOCON
akka.serialization.jackson.play.serialization-features {
  WRITE_DATES_AS_TIMESTAMPS = on
  WRITE_DURATIONS_AS_TIMESTAMPS = on
}
```

### Dropped the overrides for `akka.actor.default-dispatcher.fork-join-executor`

The overrides that Play had under `akka.actor.default-dispatcher.fork-join-executor` have been dropped in favour of using Akka's new-and-improved defaults.

See the section related to [changes in the default dispatch][akka-migration-guide-default-dispatcher] in Akka's migration guide for more details.

[akka-migration-guide-default-dispatcher]: https://doc.akka.io/docs/akka/2.6/project/migration-guide-2.5.x-2.6.x.html#default-dispatcher-size

### `IOSource` and `FileIO` changes in Akka Streams

There are changes related to how Akka Streams handle errors for `FileIO.toPath`, `StreamConverters.fromInputStream`, and `StreamConverters.fromOutputStream`. See the section related to [these changes](https://doc.akka.io/docs/akka/2.6/project/migration-guide-2.5.x-2.6.x.html#iosources-file) in Akka's migration guide for more details.

### Configuration loading changes

Until Play 2.7, when loading configuration, Play was not considering the default [Java System Properties](https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html) if the user provides some properties. Now, System Properties are always considered, meaning you can reference them in your `application.conf` file even if you are also defining custom properties. For example, when [[embedding Play|ScalaEmbeddingPlayAkkaHttp]] like the code below, both `userProperties` and System Properties are used:

```scala
import java.util.Properties

import play.api.mvc.Results
import play.core.server.AkkaHttpServer
import play.core.server.ServerConfig
import play.api.routing.sird._

class MyApp {
  def main(args: Array[String]): Unit = {
    // Define some user properties here
    val userProperties = new Properties()
    userProperties.setProperty("my.property", "some value")

    val serverConfig = ServerConfig(properties = userProperties)

    val server = AkkaHttpServer.fromRouterWithComponents(serverConfig) { components => {
      case GET(p"/hello") => components.defaultActionBuilder {
        Results.Ok
      }
    }}
  }
}
```

Keep in mind that system properties override user-defined properties.

### Debugging SSL Connections

Until Play 2.7, both Play and Play-WS were using a version of [ssl-config](https://lightbend.github.io/ssl-config/) which had a debug system that relied on undocumented modification of internal JSSE debug settings. These are usually set using `javax.net.debug` and `java.security.debug` system properties on startup.

The debug system has been removed, and debug flags that do not have a direct correlation in the new system are deprecated, and the new configuration is documented in [ssl-config docs](https://lightbend.github.io/ssl-config/DebuggingSSL.html).

### I18n behavior changes

Matching of languages behavior now satisfies [RFC 4647](https://www.ietf.org/rfc/rfc4647.txt).

#### Example

`conf/application.conf` file:

```HOCON
play.i18n.langs = [ "fr", "fr-FR", "en", "en-US" ]
```

User's accept-language:
```
en-GB
```

##### Before

User's accept-language `en-GB` does not match any of the conf. The play app responds French page against user's will.

##### After

User's accept-language `en-GB` matches `en`. The play app responds English page.

### Generalized server backend configurations

Play comes with two [[server backends|Server]]:

* [[Akka HTTP|AkkaHttpServer]] (the default), which [[can be configured|SettingsAkkaHttp]] via `play.server.akka.*`
* [[Netty|NettyServer]], which [[can be configured|SettingsNetty]] via `play.server.netty.*`.

Until now, we kept these configurations separate, even if there were settings that applied to both backends and therefore were conclusively duplicates.
With Play 2.8 we start to generalize such duplicate server backend configurations and move them directly below `play.server.*`:

* `play.server.akka.max-content-length` is now deprecated. It moved to `play.server.max-content-length`. Starting with Play 2.8 the Netty server backend will now also respect that config.
* `play.server.akka.max-header-value-length` and `play.server.netty.maxHeaderSize` are both deprecated now. Those configs moved to `play.server.max-header-size`.

### The `excludePaths` config of the Redirect HTTPS filter changed

The `play.filters.https.excludePaths` config of the [[Redirect HTTPS filter|RedirectHttpsFilter]] now contains a list of paths instead of URIs. That means query params don't matter anymore. If the list contains `/foo` the request `/foo?abc=xyz` will now be excluded too. Before Play 2.8, because the URI of a request (path + query params) was checked against the list, you needed to add _exactly_ a request's URI to exclude it from redirecting.

## Defaults changes

Some default values used by Play had changed and that can have an impact on your application. This section details the default changes.

### `Content-Disposition: inline` header not send anymore when serving files

When serving files via the [[Scala API|ScalaStream#Serving-files]] or the [[Java API|JavaStream#Serving-files]] Play by default generates the `Content-Disposition` header automatically and sends it to the client.

Starting with Play 2.8 however, when the computed header ends up being _exactly_ `Content-Disposition: inline` (when passing `inline = true`, which is the default, and `null` as file name),  it won't be send by Play automatically anymore. Because, according to [RFC 6266 Section 4.2](https://tools.ietf.org/html/rfc6266#section-4.2), rendering content inline is the default anyway.

Therefore, this change should not affect you at all, since all browsers adhere to the specs and do not treat this header in any special way but to render content inline, like no header was send.

If you still want to send this exact header however, you can still do that by using the `withHeader(s)` methods from [`Scala's`](api/scala/play/api/mvc/Result.html#withHeaders\(headers:\(String,String\)*\):play.api.mvc.Result) or [`Java's`](api/java/play/mvc/Result.html#withHeader-java.lang.String-java.lang.String-) `Result` class.

## Dependency graph changes

Until Play 2.7.0, [`"com.typesafe.play" %% "play-test"` artifact](https://mvnrepository.com/artifact/com.typesafe.play/play-test_2.13/2.7.3) was including both Akka HTTP and Netty server backends. This creates unstable behavior for your tests and, also, made them possibly use a different server than the production code, depending on how the classpath is sorted. In Play 2.8, `play-test` depends on `play-server` instead, and then the tests will use the same server provider that the application uses. If, for some reason, you need add a provider to our tests, you can do that by adding either Akka HTTP or Netty server dependencies as a `"test"` dependency. For example:

```scala
libraryDependencies += akkaHttpServer % "test" // Use nettyServer if you want the Netty Server backend
```

## Updated libraries

Besides updates to newer versions of our own libraries (play-json, play-ws, twirl, cachecontrol, etc), many other important dependencies were updated to the newest versions:

* specs2 4.8.1
* Jackson 2.10.1
* Mockito 3.2.0
* HikariCP 3.4.1
* Hibernate Validator 6.1.0.Final
* Lightbend Config 1.4.0
* Caffeine 2.8.0
* sbt-native-packager 1.5.1
