<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.4 Migration Guide

This is a guide for migrating from Play 2.3 to Play 2.4. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.3 Migration Guide|Migration23]].

As well as the information contained on this page, there are is more detailed migration information for some topics:

- [[Migrating GlobalSettings|GlobalSettings]]
- [[Migrating Plugins to Modules|PluginsToModules]]
- [[Migrating Anorm|Anorm]]

## Java 8 support

The support for Java 6 and Java 7 was dropped and Play 2.4 now requires Java 8. This decision was made based on the fact that [Java 7 reached its End-of-Life in April 2015](https://www.java.com/en/download/faq/java_7.xml). Also, Java 8 enables clean APIs and has better support for functional programming style. If you try to use Play 2.4 with Java 6/7, you will get an error like below:

```
java.lang.UnsupportedClassVersionError: play/runsupport/classloader/ApplicationClassLoaderProvider : Unsupported major.minor version 52.0
```

A [java.lang.UnsupportedClassVersionError](https://docs.oracle.com/javase/8/docs/api/java/lang/UnsupportedClassVersionError.html) means that reading a Java class file with an older version of Java than the class file was compiled with is unsupported.

> **Note:** Scala 2.10 does not have full support to all Java 8 language features, like static methods on interfaces. If your project has Java code using these new features present in Java 8, upgrade to use Scala 2.11.6+. See [sbt docs](https://www.scala-sbt.org/0.13/docs/Howto-Scala.html) to learn how to set `scalaVersion` to your project.

## Build changes

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

### Play upgrade

Update the Play version number in `project/plugins.sbt` to upgrade Play:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0") // Set to latest version of Play 2.4
```

### sbt upgrade

Play 2.4 now requires a minimum of sbt 0.13.8.  Update your `project/build.properties` so that it reads:

```
sbt.version=0.13.8
```

### Specs2 support in a separate module

If you were previously using Play's specs2 support, you now need to explicitly add a dependency on that to your project:
```scala
libraryDependencies += specs2 % Test
```
If you are using a .scala build file, you will need to add the following import `import play.sbt.PlayImport._`

### Database evolutions support in a separate module

Support for [[database evolutions|Evolutions]] used to be included with both Play JDBC and JPA support. That's no longer the case. Therefore, if you are using evolutions, you now need to add an explicit dependency to `evolutions` in your project's build:

```scala
libraryDependencies += evolutions
```

While, if you are not using evolutions, you can now safely remove `evolutionplugin=disabled` from your `application.conf`.

The configuration keys have changed, so instead of `evolutionplugin=disabled` you now need to use `play.evolutions.enabled=false` (See [[Evolutions configuration|Evolutions#Evolutions-configuration]])

If you are using Play Slick module (with or without evolutions), things have changed quite a bit, so make sure to read the [[Play Slick migration guide|PlaySlickMigrationGuide]].

### IDEs: Eclipse and IntelliJ IDEA

Play no longer includes the sbteclipse or sbt-idea plugins, which enables users to upgrade IDE support independently of Play.

Eclipse support can be setup with as little as one extra line to import the plugin. See the [[documentation|IDE]] for details.

IntelliJ is now able to import sbt projects natively, so we recommend using that instead.  Alternatively, the sbt-idea plugin can be manually installed and used, instructions can be found [here](https://github.com/mpeltonen/sbt-idea).

### Play sbt plugin API

All classes in the sbt plugin are now in the package `play.sbt`, this is particularly pertinent if using `.scala` files to configure your build. You will need to import identifiers from `play.sbt.PlayImport` to use play provided configuration elements.

#### `playWatchService` renamed

The sbt setting key `playWatchService` has been renamed to `fileWatchService`.

Also the corresponding class has changed. To set the FileWatchService to poll every two seconds, use it like this:
```scala
PlayKeys.fileWatchService := play.runsupport.FileWatchService.sbt(2000)
```

### Play Slick dependency

Play Slick module has gone through a major restyle to support Slick 3.0. For a smooth upgrade, read the [[Play Slick migration guide|PlaySlickMigrationGuide]].

### Ebean dependency

Ebean has been pulled out into an external project, to allow it to have a lifecycle independent of Play's own lifecycle.  The Ebean bytecode enhancement functionality has also been extracted out of the Play sbt plugin into its own plugin.

To migrate an existing Play project that uses Ebean to use the new external Ebean plugin, remove `javaEbean` from your `libraryDependencies` in `build.sbt`, and add the following to `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "1.0.0")
```

After that, enable Ebean plugin for your project:

```scala
lazy val myProject = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)
```

And finally, configure Ebean mapped classes as a list instead of a comma separated string (which is still supported but was deprecated):

```
ebean.default = ["models.*"]
ebean.orders = ["models.Order", "models.OrderItem"]
```

Additionally, Ebean has been upgraded to 4.5.x, which pulls in a few of the features that Play previously added itself, including the `Model` class.  Consequently, the Play `Model` class has been deprecated, in favour of using `com.avaje.ebean.Model`.

### Bytecode enhancement

[[Play's bytecode enhancement|PlayEnhancer]], which generates getters and setters for Java properties, has been pulled out of the core of Play into a separately managed project that can have its own lifecycle. To enable it, add the following to your `project/plugins.sbt` file:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.1.0")
```

## Dependency Injection

Play now, out of the box, uses dependency injection provided by Guice.  This is part of a long term strategy to remove global state out of Play, which we hope to complete in the Play 3.0 release.  Moving any application from depending on global state to being entirely global state free is a big task, one that can be very disruptive if it is done all at once.  For this reason, the approach we've taken in Play is to spread the change over a number of releases, allowing end users to gradually migrate their code so that it doesn't depend on global state, rather than forcing it all at once.

As much as practical, we have ensured that the APIs provided in Play 2.4 are source compatible with Play 2.3.  This means, in many situations, there are two ways of doing things, a way that depends on global state, and a way that doesn't.  We've updated the documentation to reflect the new dependency injection approach of doing things - in cases where you still want to use the old APIs and see documentation about them, in general, the Play 2.3 documentation is still relevant.

It's important that you read the documentation about dependency injection in Play before proceeding with migrating to Play 2.4.  There are some decisions to make up front.  Out of the box we provide and encourage the use of Guice for dependency injection, but many other dependency injection tools and techniques, including compile time dependency injection techniques in Scala are possible.  You can read about dependency injection in [[Java|JavaDependencyInjection]] or [[Scala|ScalaDependencyInjection]].

### Routing

One of the most disruptive changes with regards to dependency injection is we now support the generation of two styles of routers.  The first is the existing static style, this is largely unchanged from the Play 2.3 router.  It is a Scala singleton object, and assumes that all the actions that it invokes are either Scala singleton objects, or Java static methods.  The second is a dependency injected router, which is a class that declares its dependencies in its constructor.  To illustrate the difference between these two routers, consider the following routes file:

```
GET   /               controllers.Application.index
POST  /save           controllers.Application.save
GET   /assets/*file   controllers.Assets.versioned(path = "/public", file: Asset)
```

The static routes generator will generate a router that very roughly (pseudo code) looks like this:

```scala
object Routes extends GeneratedRouter {
  def routes = {
    case ("GET", "/") => controllers.Application.index
    case ("POST", "/save") => controllers.Application.save
    case ("GET", "/assets/:file") => controllers.Assets.versioned("/public", file)
  }
}
```

Meanwhile the injected routes generator will generate a router that very roughly looks like this:

```scala
class Routes(application: controllers.Application, assets: controllers.Assets) extends GeneratedRouter {
  def routes = {
    case ("GET", "/") => application.index
    case ("POST", "/save") => application.save
    case ("GET", "/assets/:file") => assets.versioned("/public", file)
  }
}
```

The default is to use the static routes generator.  You must use this if you are not ready to migrate all of your Java actions to be non static methods, or your Scala actions to be classes.  In most cases, this is quite straightforward to do, in Java it requires deleting the `static` keyword, in Scala it requires changing the word `object` to `class`.  The static router still supports the `@` operator, which will tell it to look up the action from a runtime `Injector`, you may find this useful if you are in a transitional period where some of your actions are static and some are injected.

If you wish to switch to the injected generator, add the following to your build settings in `build.sbt`:

```scala
routesGenerator := InjectedRoutesGenerator
```

By default Play will automatically handle the wiring of this router for you using Guice, but depending in the DI approach you're taking, you may be able to customize it.

The injected routes generator also supports the `@` operator on routes, but it has a slightly different meaning (since everything is injected), if you prefix a controller with `@`, instead of that controller being directly injected, a JSR 330 `Provider` for that controller will be injected.  This can be used, for example, to eliminate circular dependency issues, or if you want a new action instantiated per request.

In addition, Play now, by default, generates the router in the `router` package, instead of at the root package.  This is to aid with dependency injection, so if needed it can be manually created or bound, since classes in the root package can't usually be referenced.

### Dependency Injected Components

While Play 2.4 won't force you to use the dependency injected versions of components, we do encourage you to start switching to them.  The following tables show old static APIs that use global state and new injected APIs that you should be switching to:

#### Scala

| Old API | New API | Comments |
| ------- | --------| -------- |
| [`Lang`](api/scala/play/api/i18n/Lang$.html) | [`Langs`](api/scala/play/api/i18n/Langs.html) | |
| [`Messages`](api/scala/play/api/i18n/Messages$.html) | [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) | Using one of the `preferred` methods, you can get a [`Messages`](api/scala/play/api/i18n/Messages.html) instance. |
| `DB` | [`DBApi`](api/scala/play/api/db/DBApi.html) or better, [`Database`](api/scala/play/api/db/Database.html) | You can get a particular database using the `@NamedDatabase` annotation. |
| `Cache` | [`CacheApi`](api/scala/play/api/cache/CacheApi.html) or better | You can get a particular cache using the `@NamedCache` annotation. |
| `Cached` object | [`Cached` instance](api/scala/play/api/cache/Cached.html) | Use an injected instance instead of the companion object. You can use the `@NamedCache` annotation. |
| [`Akka`](api/scala/play/api/libs/concurrent/Akka$.html) | N/A | No longer needed, just declare a dependency on `ActorSystem` |
| `WS` | [`WSClient`](api/scala/play/api/libs/ws/WSClient.html) | |
| `Crypto` | `Crypto` | |
| `GlobalSettings` | [`HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html), [`HttpRequestHandler`](api/scala/play/api/http/HttpRequestHandler.html), and [`HttpFilters`](api/scala/play/api/http/HttpFilters.html)| Read the details in the [[GlobalSettings|Migration24#GlobalSettings]] section below. |

#### Java

| Old API | New API | Comments |
| ------- | --------| -------- |
| [`Lang`](api/java/play/i18n/Lang.html) | [`Langs`](api/java/play/i18n/Langs.html) | Instances of `Lang` objects are still fine to use |
| [`Messages`](api/java/play/i18n/Messages.html) | [`MessagesApi`](api/java/play/i18n/MessagesApi.html) | Using one of the `preferred` methods, you can get a `Messages` instance, and you can then use `at` to get messages for that lang. |
| `DB` | [`DBApi`](api/java/play/db/DBApi.html) or better, [`Database`](api/java/play/db/Database.html) | You can get a particular database using the [`@NamedDatabase`](api/java/play/db/NamedDatabase.html) annotation. |
| [`JPA`](api/java/play/db/jpa/JPA.html) | [`JPAApi`](api/java/play/db/jpa/JPAApi.html) | |
| `Cache` | [`CacheApi`](api/java/play/cache/CacheApi.html) | You can get a particular cache using the [`@NamedCache`](api/java/play/cache/NamedCache.html) annotation. |
| [`Akka`](api/java/play/libs/Akka.html) | N/A | No longer needed, just declare a dependency on `ActorSystem` |
| [`WS`](api/java/play/libs/ws/WS.html) | [`WSClient`](api/java/play/libs/ws/WSClient.html) | |
| `Crypto` | `Crypto` | The old static methods have been removed, an instance can statically be accessed using `play.Play.application().injector().instanceOf(Crypto.class)` |
| `GlobalSettings` | [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html), [`HttpRequestHandler`](api/java/play/http/HttpRequestHandler.html), and [`HttpFilters`](api/java/play/http/HttpFilters.html)| Read the details in the [[GlobalSettings|Migration24#GlobalSettings]] section below. |

### GlobalSettings

If you are keen to use dependency injection, we are recommending that you move out of your `GlobalSettings` implementation class as much code as possible. Read the [[`GlobalSettings` migration documentation|GlobalSettings]] for the gory details.

## `Plugin` deprecated

Both Java `play.Plugin` and Scala `play.api.Plugin` types have been deprecated. Read [[migrating Plugin to Module|PluginsToModules]] to update your code to use [`play.api.inject.Module`](api/scala/play/api/inject/Module.html).

## Configuration changes

Play 2.4 now uses `reference.conf` to document and specify defaults for all properties.  You can easily find these by going [here](https://github.com/playframework/playframework/find/master) and searching for files called `reference.conf`.

Additionally, Play has now better namespaced a large number of its configuration properties.  The old configuration paths will generally still work, but a deprecation warning will be output at runtime if you use them.  Here is a summary of the changed keys:

| Old key                   | New key                            |
| ------------------------- | ---------------------------------- |
| `application.secret`      | `play.crypto.secret`               |
| `application.context`     | `play.http.context`                |
| `session.*`               | `play.http.session.*`              |
| `flash.*`                 | `play.http.flash.*`                |
| `application.router`      | `play.http.router`                 |
| `application.langs`       | `play.i18n.langs`                  |
| `application.lang.cookie` | `play.i18n.langCookieName`         |
| `parsers.text.maxLength`  | `play.http.parser.maxMemoryBuffer` |
| `csrf`                    | `play.filters.csrf`                |
| `evolutions.*`            | `play.evolutions.*`                |
| `applyEvolutions.<db>`    | `play.evolutions.db.<db>.autoApply`|
| `ws`                      | `play.ws`                          |

### Akka configuration

Play 2.4 now has just one actor system. Before, the internal actor system was configured under `play.akka` and the Akka plugin was configured under `akka`. The new combined actor system is configured under `akka`. There is no actor system configuration under `play.akka` anymore. However, several Play specific settings are still given under the `play.akka` prefix.

If you want to change how the actor system is configured, you can set `play.akka.config = "my-akka"`, where `my-akka` is your chosen configuration prefix.

See the [[Java|JavaAkka]] or [[Scala|ScalaAkka]] Akka page for more information.

#### Thread pool configuration

Previously the two actor systems had slightly different thread pool configuration. Now that there is only one actor system, the configuration has been merged. We've also added a LIFO (stack-based) scheduling rule which should improve performance in most Play applications.

The following settings are the new defaults in Play 2.4. They've been shown to have good performance in our testing, but every application is different so you may need to tweak them or revert them to the Play 2.3 settings. You can do that by overriding any of these values in your `application.conf`. Here are the new settings:

```
akka {
  actor {
    default-dispatcher {
      fork-join-executor {
        parallelism-factor = 1.0
        parallelism-max = 24
        task-peeking-mode = LIFO
      }
    }
  }
}
```

In particular, you might want to try the [default Akka settings](https://doc.akka.io/docs/akka/2.3.11/general/configuration.html#listing-of-the-reference-configuration):

```
akka {
  actor {
    default-dispatcher {
      fork-join-executor {
        parallelism-factor = 3.0
        parallelism-max = 64
        task-peeking-mode = FIFO
      }
    }
  }
}
```

See the [[thread pool configuration section|ThreadPools#Configuring-the-Play-default-thread-pool]] for more information.

### HttpRequestHandler

The HttpRequestHandler that Play uses by default delegates to the legacy `GlobalSettings` methods. If you're not using `GlobalSettings` in your application then you can increase performance slightly by changing the handler. You can do that by adding the following to your settings:

```
play.http.requestHandler = "play.http.DefaultHttpRequestHandler"
```

### Logging

Logging is now configured solely via [logback configuration files](https://logback.qos.ch/manual/configuration.html).

## JDBC connection pool

The default JDBC connection pool is now provided by [HikariCP](http://brettwooldridge.github.io/HikariCP/), instead of BoneCP.

To switch back to BoneCP, you can set the `play.db.pool` property in `application.conf`:

```
play.db.pool = bonecp
```

The full range of configuration options available to the Play connection pools can be found in the Play JDBC [`reference.conf`](resources/confs/play-jdbc/reference.conf).

You may run into the following exception:

```
Caused by: java.sql.SQLException: JDBC4 Connection.isValid() method not supported, connection test query must be configured
```
This occurs if your JDBC-Drivers do not support Connection.isValid(). The fastest and recommended fix is to make sure you use the latest version of your JDBC-Driver. If upgrading to the latest version does not help, you may specify the `connectionTestQuery` in your application.conf like this
```
#specify a connectionTestQuery. Only do this if upgrading the JDBC-Driver does not help
db.default.hikaricp.connectionTestQuery="SELECT TRUE"
```
More information on this can be found on the [HikariCP Github Page](https://github.com/brettwooldridge/HikariCP/)

## Body Parsers

The default body parser is now `play.api.mvc.BodyParsers.parse.default`. It is similar to `anyContent` parser, except that it only parses the bodies of PATCH, POST, and PUT requests. To parse bodies for requests of other methods, explicitly pass the `anyContent` parser to `Action`.

```scala
def foo = Action(play.api.mvc.BodyParsers.parse.anyContent) { request =>
  Ok(request.body.asText)
}
```

### Maximum body length

For both Scala and Java, there have been some small but important changes to the way the configured maximum body lengths are handled and applied.

A new property, `play.http.parser.maxDiskBuffer`, specifies the maximum length of any body that is parsed by a parser that may buffer to disk.  This includes the raw body parser and the `multipart/form-data` parser.  By default this is 10MB.

In the case of the `multipart/form-data` parser, the aggregate length of all of the text data parts is limited by the configured `play.http.parser.maxMemoryBuffer` value, which defaults to 100KB.

In all cases, when one of the max length parsing properties is exceeded, a 413 response is returned.  This includes Java actions who have explicitly overridden the `maxLength` property on the `BodyParser.Of` annotation - previously it was up to the Java action to check the `RequestBody.isMaxSizeExceeded` flag if a custom max length was configured, this flag has now been deprecated.

Additionally, Java actions may now declare a `BodyParser.Of.maxLength` value that is greater than the configured max length.

## JSON API changes

The semantics of JSON lookups have changed slightly. `JsUndefined` has been removed from the `JsValue` type hierarchy and all lookups of the form `jsv \ foo` or `jsv(bar)` have been moved to `JsLookup`. They now return a `JsLookupResult` instead of a `JsValue`.

If you have code of the form

```scala
val v: JsValue = json \ "foo" \ "bar"
```

the following code is equivalent, if you know the property exists:

```scala
val v: JsValue = (json \ "foo" \ "bar").get
```

If you don't know the property exists, we recommend using pattern matching or the methods on `JsLookupResult` to safely handle the `JsUndefined` case, e.g.

```scala
val vOpt: Option[JsValue] = (json \ "foo" \ "bar").toOption
```

### JsLookup

All JSON traversal methods have been moved to the `JsLookup` class, which is implicitly applied to all values of type `JsValue` or `JsLookupResult`. In addition to the `apply`, `\`, and `\\` methods, the `head`, `tail`, and `last` methods have been added for JSON arrays. All methods except `\\` return a `JsLookupResult`, a wrapper for `JsValue` that helps with handling undefined values.

The methods `as[A]`, `asOpt[A]`, `validate[A]` also exist on `JsLookup`, so code like the below should require no source changes:

```scala
val foo: Option[FooBar] = (json \ "foo" \ "bar").asOpt[FooBar]
val bar: JsResult[Baz] = (json \ "baz").validate[Baz]
```

As a result of these changes, your code can now assume that all values of type `JsValue` are serializable to JSON.

### Reading Options

`OptionReads` is no longer available by default in 2.4. If you have code of the form `jsv.validate[Option[A]]`, you'll need to rewrite it in one of the following ways:

 1. To map both `JsNull` and an undefined lookup result to `None`, use `jsv.validateOpt[A]`. This is typically what you want.
 2. To map all validation errors to `None`, use `JsSuccess(jsv.asOpt[A])`. This was what reading an `Option` did in 2.3.
 3. To map `JsNull` to `None` and validate the value if it exists, use `jsv.validate(Reads.optionWithNull[A])`. If the value does not exist the result will be a `JsError`.

When using the functional combinators to construct your `Reads`, you will most likely want to replace

```scala
(JsPath \ "property").read[Option[String]]
```

with

```scala
(JsPath \ "property").readNullable[String]
```

This does the same as (1) above. The same goes for `Writes` with `JsPath.writeNullable` and `Format` with `JsPath.formatNullable`. Note that `readNullable` will return a `JsError` if a node could not be found before the last node, instead of `JsSuccess(None)`.

Finally, you may run into problems creating a serializer for a type that requires a `Reads` for `Option` such as `Seq[Option[String]]`. In this case, you just need to create a reads for `Option[String]` and make sure it is in scope:

```scala
implicit val optionStringReads: Reads[Option[String]] = Reads.optionWithNull[String]
```

## Testing changes

`FakeRequest` has been replaced by [`RequestBuilder`](api/java/play/mvc/Http.RequestBuilder.html).

The reverse ref router used in Java tests has been removed. Any call to `Helpers.call` that was passed a ref router can be replaced by a call to `Helpers.route` which takes either a standard reverse router reference or a `RequestBuilder`.

## Java TimeoutExceptions

If you use the Java API, the `F.Promise` class now throws unchecked [`F.PromiseTimeoutException`s](api/java/play/libs/F.PromiseTimeoutException.html) instead of Java's checked [`TimeoutException`s](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html). The `TimeoutExceptions`s which were previously used were not properly declared with the `throws` keyword. Rather than changing the API to use the `throws` keyword, which would mean users would have to declare `throws` on their methods, the exception was changed to a new unchecked type instead. See [#1227](https://github.com/playframework/playframework/pull/1227) for more information.

| Old API | New API | Comments |
| ------- | --------| -------- |
| [`TimeoutException`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html) | [`F.PromiseTimeoutException`](api/java/play/libs/F.PromiseTimeoutException.html) | |

## WS client

`WSRequestHolder` has been renamed to `WSRequest` in [Scala](api/scala/play/api/libs/ws/WSRequest.html) and [Java](api/java/play/libs/ws/WSRequest.html).  The previous `WSRequest` class has been removed out as it was only used internally to WS for OAuth functionality.

WS has upgraded from AsyncHttpClient 1.8.x to 1.9.x, which includes a number of breaking changes if using or configuring that library directly.  Please see the [AsyncHttpClient Migration Guide](https://github.com/AsyncHttpClient/async-http-client/blob/2.0/MIGRATION.md) for more details.  The upgrade to AsyncHttpClient 1.9.x enables Server Name Indication (SNI) in HTTPS -- this solves a number of problems with HTTPS based CDNs such as Cloudflare which depend heavily on SNI.

Configuration settings for WS have changed:

* `ws.acceptAnyCertificate` has been moved under the loose settings as `play.ws.ssl.loose.acceptAnyCertificate` to better indicate the insecure nature of blindly accepting any X.509 certificate without validation.
* `ws.ssl.debug` settings have been redefined as booleans, e.g. `play.ws.ssl.debug.all=true`.  Please see [Debugging SSL](https://www.playframework.com/documentation/2.4.x/DebuggingSSL) for details.
* `ws.ssl.disabledSignatureAlgorithms` and `ws.ssl.disabledKeyAlgorithms` have been redefined as arrays of strings, e.g `play.ws.ssl.disabledSignatureAlgorithms = ["MD2", "MD4", "MD5"]`.

Because of the AsyncHttpClient 1.9.x upgrade, several settings no longer have the same names that they did previously in the 1.8.x version AsyncHttpClientConfig.Builder.  To reduce confusion, here is the map from WS settings to 1.9.x AsyncHttpClientConfig.Builder:

* `play.ws.ning.connectionTimeout` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setConnectTimeout(int)">setConnectTimeout</a>
* `play.ws.ning.idleTimeout` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setReadTimeout(int)">setReadTimeout</a>
* `play.ws.ning.followRedirects` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setFollowRedirect(boolean)">setFollowRedirect</a>
* `play.ws.ning.compressionEnabled` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setCompressionEnforced(boolean)">setCompressionEnforced</a>
* `play.ws.ning.allowPoolingConnection` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setAllowPoolingConnections(boolean)">setAllowPoolingConnections</a>
* `play.ws.ning.allowSslConnectionPool` ->  <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setAllowPoolingSslConnections(boolean)">setAllowPoolingSslConnections</a>
* `play.ws.ning.maxConnectionsTotal` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setMaxConnections(int)">setMaxConnections</a>
* `play.ws.ning.maxConnectionLifetime` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setConnectionTTL(int)">setConnectionTTL</a>
* `play.ws.ning.idleConnectionInPoolTimeout` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setPooledConnectionIdleTimeout(int)">setPooledConnectionIdleTimeout</a>
* `play.ws.ning.webSocketIdleTimeout` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setWebSocketTimeout(int)">setWebSocketTimeout</a>
* `play.ws.ning.maxNumberOfRedirects` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setMaxRedirects(int)">setMaxRedirects</a>
* `play.ws.ning.disableUrlEncoding` -> <a href="http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/AsyncHttpClientConfig.Builder.html#setDisableUrlEncodingForBoundedRequests(boolean)">setDisableUrlEncodingForBoundedRequests</a>

WS has changed the OAuth signature calculator from [Signpost](https://github.com/mttkay/signpost) to AsyncHttpClient's [OAuthCalculator](http://static.javadoc.io/com.ning/async-http-client/1.9.22/com/ning/http/client/oauth/OAuthSignatureCalculator.html).  Signpost is still used to retrieve the request token and access tokens.  This should not require any application level changes, but is worth noting in case of unexpected OAuth failures.

Due to the recent spate of TLS vulnerabilities, there has been more activity to deprecate insecure HTTPS configurations.  Per [RFC 7465](https://tools.ietf.org/html/rfc7465), RC4 cipher suites have added to the list of deprecated ciphers, and are not available by default.  They may be explicitly enabled as cipher suites using the `play.ws.ssl.enabledCiphers` and `play.ws.ssl.loose.allowWeakCiphers` settings.  Please also consider reviewing [RFC 7525](https://tools.ietf.org/html/rfc7525) for the IETF recommended configuration of TLS.

## Crypto APIs

Play 2.4's AES encryption now uses [initialization vectors](https://en.wikipedia.org/wiki/Initialization_vector) to randomize each encryption. The Play encryption format has been changed to add support for initialization vectors.

The full name of the new AES transformation used by Play 2.4 is `AES/CTR/NoPadding`. The old transformation was `AES/ECB/PKCS5Padding`. The [`CTR`](https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Counter_.28CTR.29) mode is much more secure than the `ECB` mode. As before, you can override Play's encryption transformation by setting the `play.crypto.aes.transformation` configuration option. In Play 2.4, any [transformation supported by your JRE](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Cipher) can be used, including transformations that use an initialization vector.

Play 2.4 uses a new encryption format, but it can read data encrypted by earlier versions of Play. However, earlier versions of Play **will not** be able to read data encrypted by Play 2.4. If your Play 2.4 application needs to produce data in the old format then you may want to copy the algorithm from the [Play 2.3 Crypto code](https://github.com/playframework/playframework/blob/2.3.6/framework/src/play/src/main/scala/play/api/libs/Crypto.scala#L187-L277).

The table below shows the encryption formats supported by different versions of Play. _Old format_ is used by older versions of Play. _New format I_ is used by Play 2.4 if the configured cipher doesn't use an initialization vector. _New format II_ is used when an initialization vector is needed.

Format | Encoding | Play 2.3 || Play 2.4 ||
  ---- | ----
Old format | _hex(cipher(plaintext))_ | writes | reads | | reads
New format I | "1-" + _base64(cipher(plaintext))_ | | | writes | reads
New format II | "2-" + _base64(iv + cipher(plaintext, iv))_ | | | writes | reads

Usage of the Java Crypto API remains the same even though the output is different:

```java
import play.libs.Crypto;

String enc = Crypto.encryptAES(orig);
String dec = Crypto.decryptAES(enc);
```

Usage of the Scala Crypto API is also the same:

```scala
import play.api.libs.Crypto

val enc = Crypto.encryptAES(orig)
val dec = Crypto.decryptAES(enc)
```
## Anorm

The new Anorm version includes various fixes and improvements. Read [[here|Anorm]] to find out more.

## HTTP server configuration

Advanced Netty configuration options, that is, options prefixed with `http.netty.option`, must now use the prefix `play.server.netty.option` instead.

## I18n

The configuration key to specify the languages that your application supports changed from `application.langs` to `play.i18n.langs`. Also, it is now a list instead of a comma separated string. Per instance:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

### Scala

You now need to have an implicit [`Messages`](api/scala/play/api/i18n/Messages.html) value instead of just `Lang` in order to use the i18n API. The `Messages` type aggregates a `Lang` and a [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html).

This means that you should change your templates to take an implicit `Messages` parameter instead of `Lang`:

```scala
@(form: Form[Login])(implicit messages: Messages)
...
```

From you controllers you can get such an implicit `Messages` value by mixing the [`play.api.i18n.I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) trait in your controller that gives you an implicit `Messages` value as long as there is a `RequestHeader` value in the implicit scope. The `I18nSupport` trait has an abstract member `def messagesApi: MessagesApi` so your code will typically look like the following:

```scala
import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.Controller

class MyController @Inject() (val messagesApi: MessagesApi)
  extends Controller with I18nSupport {

}
```

A simpler migration path is also supported if you want your controller to be still use static controller objects rather than injected classes with the `I18nSupport` trait. After modifying your templates to take an implicit `Messages` parameter, as described above, add the following import to your controllers:

```scala
import play.api.i18n.Messages.Implicits._
```

This import brings you an implicit `Messages` value as long as there are a `Lang` and an `Application` in the implicit scope (thankfully controllers already provide the `Lang` and you can get the currently running application by importing `play.api.Play.current`).

### Java

The API should be backward compatible with your code using Play 2.3 so there is no migration step. Nevertheless, note that you have to start your Play application before using the Java i18n API. That should always be the case when you run your project, however your test code may not always start your application. Please refer to the corresponding [[documentation page|JavaTest]] to know how to start your application before running your tests.

## Distribution

Previously, Play added all the resources to the `conf` directory in the distribution, but didn't add the `conf` directory to the classpath.  Now Play adds the `conf` directory to the classpath by default.

This can be turned off by setting `PlayKeys.externalizeResources := false`, which will cause no `conf` directory to be created in the distribution, and it will not be on the classpath.  The contents of the applications `conf` directory will still be on the classpath by virtue of the fact that it's included in the applications jar file.

Please note that if you're using the Java Persistence API (JPA) you will need to set this option to false in order to deploy your application. Otherwise you will get an error message: `Entity X is not mapped`. This is not required for local development.

### Changes in Debian Package creation

The [sbt-native-packager](https://github.com/sbt/sbt-native-packager) has been upgraded. Due to this, the following adjustments might be necessary:
 * The syntax of the `/etc/default/$appname` file has changed from being a simple list of command line parameters to being a shell script that gets sourced by the start/stop scripts, allowing you to set environment variables.
 * The equivalent to the old syntax of the default file is an `application.ini` file in your archive's `conf` folder.
 * The default-file gets sourced by `SystemV` Init scripts only - Upstart ignores this file right now. To change your build to create `SystemV` compatible packages, add this to your build.sbt:

```
import com.typesafe.sbt.packager.archetypes.ServerLoader.{SystemV, Upstart}

serverLoading in Debian := SystemV
```

 * Other changes that might be necessary can be found in the [sbt-native-packager release notes](https://github.com/sbt/sbt-native-packager/releases).


## Miscellaneous

### No more OrderedExecutionContext

The mysterious `OrderedExecutionContext` had [[been retained|Migration22#Concurrent-F.Promise-execution]] in Play for several versions in order to support legacy applications. It was rarely used and has now been removed. If you still need the `OrderedExecutionContext` for some reason, you can create your own implementation based on the [Play 2.3 source](https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/core/j/OrderedExecutionContext.scala). If you haven't heard of this class, then there's nothing you need to do.

### Subproject Assets

Any assets in sub projects are now by default placed into /lib/[subproject] to allow files with the same name in the root project / different subprojects without causing them to interfere with each other.

To get the asset routing to work correctly in your app, you'll need to change:

    GET     /assets/*file               controllers.myModule.Assets.at(path="/public", file)

to this

    GET     /assets/*file               controllers.myModule.Assets.at(path="/public/lib/myModule", file)
