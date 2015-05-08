<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.4 Migration Guide

This is a guide for migrating from Play 2.3 to Play 2.4. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.3 Migration Guide|Migration23]].

## Java 8 support

The support for Java 6 and Java 7 was dropped and Play 2.4 now requires Java 8. This decision was made based on the fact that [Java 7 reached its End-of-Life in April 2015](https://www.java.com/en/download/faq/java_7.xml). Also, Java 8 enables clean APIs and has better support for functional programming style. If you try to use Play 2.4 with Java 6/7, you will get an error like below:

```
java.lang.UnsupportedClassVersionError: play/runsupport/classloader/ApplicationClassLoaderProvider : Unsupported major.minor version 52.0
```

A [java.lang.UnsupportedClassVersionError](https://docs.oracle.com/javase/8/docs/api/java/lang/UnsupportedClassVersionError.html) means that reading a Java class file with an older version of Java than the class file was compiled with is unsupported.

## Build changes

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

### Play upgrade

Update the Play version number in `project/plugins.sbt` to upgrade Play:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")
```

### sbt upgrade

Play 2.4 now requires a minimum of sbt 0.13.8.  Update your `project/build.properties` so that it reads:

```
sbt.version=0.13.8
```

### Specs2 support in a separate module

If you were previously using Play's specs2 support, you now need to explicitly add a dependency on that to your project.  Additionally, specs2 now requires `scalaz-stream` which isn't available on maven central or any other repositories that sbt uses by default, so you need to add the `scalaz-stream` repository as a resolver:

```scala
libraryDependencies += specs2 % Test

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
```

### IDEs: Eclipse and IntelliJ IDEA

Play no longer includes the sbteclipse or sbt-idea plugins, which enables users to upgrade IDE support independently of Play.

Eclipse support can be setup with as little as one extra line to import the plugin. See the [[documentation|IDE]] for details.

IntelliJ is now able to import sbt projects natively, so we recommend using that instead.  Alternatively, the sbt-idea plugin can be manually installed and used, instructions can be found [here](https://github.com/mpeltonen/sbt-idea).

### Play SBT plugin API

The SBT setting key `playWatchService` has been renamed to `fileWatchService`.

Also the corresponding class has changed. To set the FileWatchService to poll every two seconds, use it like this:
```scala
PlayKeys.fileWatchService := play.runsupport.FileWatchService.sbt(2000)
```

All classes in the SBT plugin are now in the package `play.sbt`, this is particularly pertinent if using `.scala` files to configure your build..

### Ebean dependency

Ebean has been pulled out into an external project, to allow it to have a lifecycle independent of Play's own lifecycle.  The Ebean bytecode enhancement functionality has also been extracted out of the Play sbt plugin into its own plugin.

To migrate an existing Play project that uses Ebean to use the new external Ebean plugin, remove `javaEbean` from your `libraryDependencies` in `build.sbt`, and add the following to `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "1.0.0")
```

After that, enable Ebean plugin for your project:

```scala
lazy val myProject = (project in file("."))
  .enablePlugins(PlayJava, SbtEbean)
```

And finally, configure Ebean mapped classes as a list instead of a comma separated string (which is still supported but was deprecated):

```
ebean.default = ["models.*"]
ebean.orders = ["models.Order", "models.OrderItem"]
```

Additionally, Ebean has been upgraded to 4.5.x, which pulls in a few of the features that Play previously added itself, including the `Model` class.  Consequently, the Play `Model` class has been deprecated, in favour of using `com.avaje.ebean.Model`.

### Anorm dependency

Anorm has been pulled out of the core of Play into a separately managed project that can have its own lifecycle.  To add a dependency on it, use:

```scala
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.4.0"
```

For more details about what's changed in Anorm, see [[here|Migration24#Anorm]].

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

By default Play will automatically handle the wiring of this router for you using Guice, but depending in the DI approach you're taking, you may be able to customise it.

The injected routes generator also supports the `@` operator on routes, but it has a slightly different meaning (since everything is injected), if you prefix a controller with `@`, instead of that controller being directly injected, a JSR 330 `Provider` for that controller will be injected.  This can be used, for example, to eliminate circular dependency issues, or if you want a new action instantiated per request.

In addition, Play now, by default, generates the router in the `router` package, instead of at the root package.  This is to aid with dependency injection, so if needed it can be manually created or bound, since classes in the root package can't usually be referenced.

### Dependency Injected Components

While Play 2.4 won't force you to use the dependency injected versions of components, we do encourage you to start switching to them.  The following tables show old static APIs that use global state and new injected APIs that you should be switching to:

#### Scala

| Old API | New API | Comments |
| ------- | --------| -------- |
| [`Lang`](api/scala/index.html#play.api.i18n.Lang$) | [`Langs`](api/scala/index.html#play.api.i18n.Langs) | |
| [`Messages`](api/scala/index.html#play.api.i18n.Messages$) | [`MessagesApi`](api/scala/index.html#play.api.i18n.MessagesApi) | Using one of the `preferred` methods, you can get a [`Messages`](api/scala/index.html#play.api.i18n.Messages) instance. |
| [`DB`](api/scala/index.html#play.api.db.DB$) | [`DBApi`](api/scala/index.html#play.api.db.DBApi) or better, [`Database`](api/scala/index.html#play.api.db.Database) | You can get a particular database using the `@NamedDatabase` annotation. |
| [`Cache`](api/scala/index.html#play.api.cache.Cache$) | [`CacheApi`](api/scala/index.html#play.api.cache.CacheApi) or better | You can get a particular cache using the `@NamedCache` annotation. |
| [`Cached` object](api/scala/index.html#play.api.cache.Cached$) | [`Cached` instance](api/scala/index.html#play.api.cache.Cached) | Use an injected instance instead of the companion object. You can use the `@NamedCache` annotation. |
| [`Akka`](api/scala/index.html#play.api.libs.concurrent.Akka$) | N/A | No longer needed, just declare a dependency on `ActorSystem` |
| [`WS`](api/scala/index.html#play.api.libs.ws.WS$) | [`WSClient`](api/scala/index.html#play.api.libs.ws.WSClient) | |
| [`Crypto`](api/scala/index.html#play.api.libs.Crypto$) | [`Crypto`](api/scala/index.html#play.api.libs.Crypto) | |

#### Java

| Old API | New API | Comments |
| ------- | --------| -------- |
| [`Lang`](api/java/play/i18n/Lang.html) | [`Langs`](api/java/play/i18n/Langs.html) | Instances of `Lang` objects are still fine to use |
| [`Messages`](api/java/play/i18n/Messages.html) | [`MessagesApi`](api/java/play/i18n/MessagesApi.html) | Using one of the `preferred` methods, you can get a `Messages` instance, and you can then use `at` to get messages for that lang. |
| [`DB`](api/java/play/db/DB.html) | [`DBApi`](api/java/play/db/DBApi.html) or better, [`Database`](api/java/play/db/Database.html) | You can get a particular database using the [`@NamedDatabase`](api/java/play/db/NamedDatabase.html) annotation. |
| [`JPA`](api/java/play/db/jpa/JPA.html) | [`JPAApi`](api/java/play/db/jpa/JPAApi.html) | |
| [`Cache`](api/java/play/cache/Cache.html) | [`CacheApi`](api/java/play/cache/CacheApi.html) | You can get a particular cache using the [`@NamedCache`](api/java/play/cache/NamedCache.html) annotation. |
| [`Akka`](api/java/play/libs/Akka.html) | N/A | No longer needed, just declare a dependency on `ActorSystem` |
| [`WS`](api/java/play/libs/ws/WS.html) | [`WSClient`](api/java/play/libs/ws/WSClient.html) | |
| [`Crypto`](api/java/play/libs/Crypto.html) | [`Crypto`](api/java/play/libs/Crypto.html) | The old static methods have been removed, an instance can statically be accessed using `play.Play.application().injector().instanceOf(Crypto.class)` |

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

### Logging

Logging is now configured solely via [logback configuration files](http://logback.qos.ch/manual/configuration.html).

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

The semantics of JSON lookups have changed slightly. `JsUndefined` has been removed from the `JsValue` type hierarchy and all lookups of the form `jsv \ foo` or `jsv(bar)` have been moved to [`JsLookup`](api/java/play/api/libs/json/JsLookup.html). They now return a [`JsLookupResult`](api/java/play/api/libs/json/JsLookupResult.html) instead of a `JsValue`.

If you have code of the form

```scala
val v: JsValue = json \ "foo" \ "bar"
```

the following code is equivalent, if you know the property exists:

```scala
val v: JsValue = (json \ "foo" \ "bar").get
```

If you don't know the property exists, we recommend using pattern matching or the methods on [`JsLookupResult`](api/java/play/api/libs/json/JsLookupResult.html) to safely handle the `JsUndefined` case, e.g.

```scala
val vOpt = Option[JsValue] = (json \ "foo" \ "bar").toOption
```

### JsLookup

All JSON traversal methods have been moved to the [`JsLookup`](api/java/play/api/libs/json/JsLookup.html) class, which is implicitly applied to all values of type `JsValue` or `JsLookupResult`. In addition to the `apply`, `\`, and `\\` methods, the `head`, `tail`, and `last` methods have been added for JSON arrays. All methods except `\\` return a [`JsLookupResult`](api/java/play/api/libs/json/JsLookupResult.html), a wrapper for `JsValue` that helps with handling undefined values.

The methods `as[A]`, `asOpt[A]`, `validate[A]` also exist on `JsLookup`, so code like the below should require no source changes:

```scala
val foo: Option[FooBar] = (json \ "foo" \ "bar").asOpt[FooBar]
val bar: JsResult[Baz] = (json \ "baz").validate[Baz]
```

As a result of these changes, your code can now assume that all values of type `JsValue` are serializable to JSON.

## Testing changes

[`FakeRequest`](api/java/play/test/FakeRequest.html) has been replaced by [`RequestBuilder`](api/java/play/mvc/Http.RequestBuilder.html).

The reverse ref router used in Java tests has been removed. Any call to `Helpers.call` that was passed a ref router can be replaced by a call to `Helpers.route` which takes either a standard reverse router reference or a `RequestBuilder`.

## Java TimeoutExceptions

If you use the Java API, the [`F.Promise`](api/java/play/libs/F.Promise.html) class now throws unchecked [`F.PromiseTimeoutException`s](api/java/play/libs/F.PromiseTimeoutException.html) instead of Java's checked [`TimeoutException`s](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html). The `TimeoutExceptions`s which were previously used were not properly declared with the `throws` keyword. Rather than changing the API to use the `throws` keyword, which would mean users would have to declare `throws` on their methods, the exception was changed to a new unchecked type instead. See [#1227](https://github.com/playframework/playframework/pull/1227) for more information.

| Old API | New API | Comments |
| ------- | --------| -------- |
| [`TimeoutException`](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html) | [`F.PromiseTimeoutException`](api/java/play/libs/F.PromiseTimeoutException.html) | |

## WS client

`WSRequestHolder` has been renamed to `WSRequest` in [Scala](api/scala/index.html#play.api.libs.ws.WSRequest) and [Java](api/java/play/libs/ws/WSRequest.html).

Play has upgraded from AsyncHttpClient 1.8 to 1.9, which includes a number of breaking changes if using or configuring that library directly.  Please see the [AsyncHttpClient Migration Guide](https://github.com/AsyncHttpClient/async-http-client/blob/master/MIGRATION.md) for more details.

## Crypto APIs

Play's Crypto API improves security by supporting encryption methods that use initialization vectors and by changing the default encryption transformation to `AES/CTR/NoPadding`. To add this support, the Play 2.4 encryption format has changed slightly. This means that cookies and other data encrypted in Play 2.4 will not be readable by older versions of Play. However, Play 2.4 can read cookies and other data encrypted in both the old and new format.

The crypto transformation is configured in `play.crypto.aes.transformation` and the default value has changed from `AES` to `AES/CTR/NoPadding`, which is more secure.

When you call `Crypto.encryptAES` in Play 2.4 it will use the configured transformation (default `AES/CTR/NoPadding`) to encrypt the data and then encode the result in the new format that supports initialization vectors.

When you call `Crypto.decryptAES` it will decode both the old and new formats. The old format is always decoded using the AES transformation. The new format is decoded using the configured transformation (default `AES/CTR/NoPadding`).

If you wish to continue using the older format of encryption decryption, here is the [link] (https://github.com/playframework/playframework/blob/2.3.6/framework/src/play/src/main/scala/play/api/libs/Crypto.scala#L187-L277) that provides all the necessary information.

## Anorm

The new Anorm version includes various fixes and improvements.

Following [BatchSQL #3016](https://github.com/playframework/playframework/commit/722cd55a3a5369f911f5d11f7c93ba4bf100ca23), `SqlQuery` case class is refactored as a trait with companion object.
Consequently, `BatchSql` is now created by passing a raw statement which is validated internally.

```scala
import anorm.BatchSql

// Before
BatchSql(SqlQuery("SQL")) // No longer accepted (won't compile)

// Now
BatchSql("SQL")
// Simpler and safer, as SqlQuery is created&validated internally
```

### Parsing

It's now possible to get values from `Row` using the column index.

```scala
val res: (String, String) = SQL("SELECT * FROM Test").map(row =>
 row[String](1) -> row[String](2) // string columns #1 and #2
)
```

Column resolution per label is now unified, whatever the label is name or alias.

```scala
val res: (String, Int) = SQL"SELECT text, count AS i".map(row =>
  row[String]("text") -> row[Int]("i")
)
```

New `fold` and `foldWhile` functions to work with result stream.

```scala
val countryCount: Either[List[Throwable], Long] =
  SQL"Select count(*) as c from Country".fold(0l) { (c, _) => c + 1 }

val books: Either[List[Throwable], List[String]] =
 SQL("Select name from Books").foldWhile(List[String]()) { (list, row) =>
  foldWhile(List[String]()) { (list, row) =>
    if (list.size == 100) (list -> false) // stop with `list`
    else (list := row[String]("name")) -> true // continue with one more name
  }
```

New `withResult` function to provide custom stream parser.

```scala
import anorm.{ Cursor, Row }
@annotation.tailrec
def go(c: Option[Cursor], l: List[String]): List[String] = c match {
  case Some(cursor) => {
    if (l.size == 100) l // custom limit, partial processing
    else {
      val row = it.next()
      go(it, l :+ row[String]("name"))
    }
  }
  case _ => l
}

val books: Either[List[Throwable], List[String]] =
  SQL("Select name from Books").withResult(go(_, List.empty[String]))
```

### Type mappings

More parameter and column conversions are available.

**Array**

A column can be multi-value if its type is JDBC array (`java.sql.Array`). Now Anorm can map it to either array or list (`Array[T]` or `List[T]`), provided type of element (`T`) is also supported in column mapping.

```scala
import anorm.SQL
import anorm.SqlParser.{ scalar, * }

// array and element parser
import anorm.Column.{ columnToArray, stringToArray }

val res: List[Array[String]] =
  SQL("SELECT str_arr FROM tbl").as(scalar[Array[String]].*)
```

New convenient parsing functions are also provided for arrays with `SqlParser.array[T](...)` and `SqlParser.list[T](...)`

In case JDBC statement is expecting an array parameter (`java.sql.Array`), its value can be passed as `Array[T]`, as long as element type `T` is a supported one.

```scala
val arr = Array("fr", "en", "ja")
SQL"UPDATE Test SET langs = $arr".execute()
```

**Multi-value parameter**

New conversions are available to pass `List[T]`, `Set[T]`, `SortedSet[T]`, `Stream[T]` and `Vector[T]` as multi-value parameter.

```scala
SQL("SELECT * FROM Test WHERE cat IN ({categories})")
 .on('categories -> List(1, 3, 4))

SQL("SELECT * FROM Test WHERE cat IN ({categories})")
 .on('categories -> Set(1, 3, 4))

SQL("SELECT * FROM Test WHERE cat IN ({categories})")
 .on('categories -> SortedSet("a", "b", "c"))

SQL("SELECT * FROM Test WHERE cat IN ({categories})")
 .on('categories -> Stream(1, 3, 4))

SQL("SELECT * FROM Test WHERE cat IN ({categories})")
 .on('categories -> Vector("a", "b", "c"))
```

**Numeric and boolean types**

Column conversions for basic types like numeric and boolean ones have been improvided.

Some invalid conversions are removed:

| Column (JDBC type) | (as) JVM/Scala type  |
| -------------------|--------------------- |
| `Double`           | `Boolean`            |
| `Int`              | `Boolean`            |

There are new conversions extending column support.

| Column (JDBC type) | (as) JVM/Scala type  |
| -------------------|--------------------- |
| `BigDecimal`       | `BigInteger`         |
| `BigDecimal`       | `Int`                |
| `BigDecimal`       | `Long`               |
| `BigInteger`       | `BigDecimal`         |
| `BigInteger`       | `Int`                |
| `BigInteger`       | `Long`               |
| `Boolean`          | `Int`                |
| `Boolean`          | `Long`               |
| `Boolean`          | `Short`              |
| `Byte`             | `BigDecimal`         |
| `Float`            | `BigDecimal`         |
| `Int`              | `BigDecimal`         |
| `Long`             | `Int`                |
| `Short`            | `BigDecimal`         |

**Binary and large data**

New column conversions are provided for binary columns (bytes, stream, blob), to be parsed as `Array[Byte]` or `InputStream`.

| ↓JDBC / JVM➞            | Array[Byte] | InputStream<sup>1</sup> |
| ----------------------- | ----------- | ----------------------- |
| Array[Byte]             | Yes         | Yes                     |
| Blob<sup>2</sup>        | Yes         | Yes                     |
| Clob<sup>3</sup>        | No          | No                      |
| InputStream<sup>4</sup> | Yes         | Yes                     |
| Reader<sup>5</sup>      | No          | No                      |

- 1. Type `java.io.InputStream`.
- 2. Type `java.sql.Blob`.
- 3. Type `java.sql.Clob`.
- 4. Type `java.io.Reader`.

Binary and large data can also be used as parameters:

| JVM                     | JDBC           |
| ------------------------|--------------- |
| Array[Byte]             | Long varbinary |
| Blob<sup>1</sup>        | Blob           |
| InputStream<sup>2</sup> | Long varbinary |
| Reader<sup>3</sup>      | Long varchar   |

- 1. Type `java.sql.Blob`
- 2. Type `java.io.InputStream`
- 3. Type `java.io.Reader`

**Misc**

- **Joda Time**: New conversions for Joda `Instant` or `DateTime`, from `Long`, `Date` or `Timestamp` column.
- Parses text column as `UUID` value: `SQL("SELECT uuid_as_text").as(scalar[UUID].single)`.
- Passing `None` for a nullable parameter is deprecated, and typesafe `Option.empty[T]` must be use instead.


## HTTP server configuration

Advanced Netty configuration options, that is, options prefixed with `http.netty.option`, must now use the prefix `play.server.netty.option` instead.

## I18n

The configuration key to specify the languages that your application supports changed from `application.langs` to `play.i18n.langs`. Also, it is now a list instead of a comma separated string. Per instance:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

### Scala

You now need to have an implicit [`Messages`](api/scala/index.html#play.api.i18n.Messages) value instead of just `Lang` in order to use the i18n API. The `Messages` type aggregates a `Lang` and a [`MessagesApi`](api/scala/index.html#play.api.i18n.MessagesApi).

This means that you should change your templates to take an implicit `Messages` parameter instead of `Lang`:

```scala
@(form: Form[Login])(implicit messages: Messages)
...
```

From you controllers you can get such an implicit `Messages` value by mixing the [`play.api.i18n.I18nSupport`](api/scala/index.html#play.api.i18n.I18nSupport) trait in your controller that gives you an implicit `Messages` value as long as there is a `RequestHeader` value in the implicit scope. The `I18nSupport` trait has an abstract member `def messagesApi: MessagesApi` so your code will typically look like the following:

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

Previously, Play added all the resources to the the `conf` directory in the distribution, but didn't add the `conf` directory to the classpath.  Now Play adds the `conf` directory to the classpath by default.

This can be turned off by setting `PlayKeys.externalizeResources := false`, which will cause no `conf` directory to be created in the distribution, and it will not be on the classpath.  The contents of the applications `conf` directory will still be on the classpath by virtue of the fact that it's included in the applications jar file.


## Miscellaneous

### No more OrderedExecutionContext

The mysterious `OrderedExecutionContext` had been retained in Play for several versions in order to support legacy applications. It was rarely used and has now been removed. If you still need the `OrderedExecutionContext` for some reason, you can create your own implementation based on the [Play 2.3 source](https://github.com/playframework/playframework/blob/2.3.x/framework/src/play/src/main/scala/play/core/j/OrderedExecutionContext.scala). If you haven't heard of this class, then there's nothing you need to do.
