<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.4 Migration Guide

This is a guide for migrating from Play 2.3 to Play 2.4. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.3 Migration Guide|Migration23]].

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
object Routes extends Router.Routes {
  def routes = {
    case ("GET", "/") => controllers.Application.index
    case ("POST", "/save") => controllers.Application.save
    case ("GET", "/assets/:file") => controllers.Assets.versioned("/public", file)
  }
}
```

Meanwhile the injected routes generator will generate a router that very roughly looks like this:

```scala
class Routes(application: controllers.Application, assets: controllers.Assets) extends Router.Routes {
  def routes = {
    case ("GET", "/") => application.index
    case ("POST", "/save") => application.save
    case ("GET", "/assets/:file") => assets.versioned("/public", file)
  }
}
```

The default is to use the static routes generator.  You must use this if you are not ready to migrate all of your Java actions to be non static methods, or your Scala actions to be classes.  In most cases, this is quite straight forward to do, in Java it requires deleting the `static` keyword, in Scala it requires changing the word `object` to `class`.  The static router still supports the `@` operator, which will tell it to look up the action from a runtime `Injector`, you may find this useful if you are in a transitional period where some of your actions are static and some are injected.

If you wish to switch to the injected generator, add the following to your build settings in `build.sbt`:

```scala
routesGenerator := InjectedRoutesGenerator
```

By default Play will automatically handle the wiring of this router for you using Guice, but depending in the DI approach you're taking, you may be able to customise it.

The injected routes generator also supports the `@` operator on routes, but it has a slightly different meaning (since everything is injected), if you prefix a controller with `@`, instead of that controller being directly injected, a JSR 330 `Provider` for that controller will be injected.  This can be used, for example, to eliminate circular dependency issues, or if you want a new action instantiated per request.

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

## Ebean

Ebean has been pulled out into an external project, to allow it to have a lifecycle independent of Play's own lifecycle.  The ebean bytecode enhancement functionality has also been extracted out of the Play sbt plugin into its own plugin.

To migrate an existing Play project that uses ebean to use the new external ebean plugin, remove `javaEbean` from your `libraryDependencies` in `build.sbt`, and add the following to `project/plugins.sbt`:

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "1.0.0")
```

Additionally, Ebean has been upgraded to 4.2.0, which pulls in a few of the features that Play previously added itself, including the `Model` class.  Consequently, the Play `Model` class has been deprecated, in favour of using `org.avaje.ebean.Model`.

## Body Parsers

The default body parser is now `play.api.mvc.BodyParsers.parse.default`. It is similar to `anyContent` parser, except that it only parses the bodies of PATCH, POST, and PUT requests. To parse bodies for requests of other methods, explicitly pass the `anyContent` parser to `Action`.

```scala
def foo = Action(play.api.mvc.BodyParsers.parse.anyContent) { request =>
  Ok(request.body.asText)
}
```

### Maximum body length

For both Scala and Java, there have been some small but important changes to the way the configured maximum body lengths are handled and applied.

A new property, `parsers.disk.maxLength`, specifies the maximum length of any body that is parsed by a parser that may buffer to disk.  This includes the raw body parser and the `multipart/form-data` parser.  By default this is 10MB.

In the case of the `multipart/form-data` parser, the aggregate length of all of the text data parts is limited by the configured `parsers.text.maxLength` value, which defaults to 100KB.

In all cases, when one of the max length parsing properties is exceeded, a 413 response is returned.  This includes Java actions who have explicitly overridden the `maxLength` property on the `BodyParser.Of` annotation - previously it was up to the Java action to check the `RequestBody.isMaxSizeExceeded` flag if a custom max length was configured, this flag has now been deprecated.

Additionally, Java actions may now declare a `BodyParser.Of.maxLength` value that is greater than the configured max length.

### Java TimeoutExceptions

If you use the Java API, the [`F.Promise`](api/java/play/libs/F.Promise.html) class now throws unchecked [`F.PromiseTimeoutException`s](api/java/play/libs/F.PromiseTimeoutException.html) instead of Java's checked [`TimeoutException`s](http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/TimeoutException.html). The `TimeoutExceptions`s which were previously used were not properly declared with the `throws` keyword. Rather than changing the API to use the `throws` keyword, which would mean users would have to declare `throws` on their methods, the exception was changed to a new unchecked type instead. See [#1227](https://github.com/playframework/playframework/pull/1227) for more information.

| Old API | New API | Comments |
| ------- | --------| -------- |
| [`TimeoutException`](http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/TimeoutException.html) | [`F.PromiseTimeoutException`](api/java/play/libs/F.PromiseTimeoutException.html) | |

### Crypto APIs

Play's Crypto API improves security by supporting encryption methods that use initialization vectors and by changing the default encryption transformation to `AES/CTR/NoPadding`. To add this support, the Play 2.4 encryption format has changed slightly. This means that cookies and other data encrypted in Play 2.4 will not be readable by older versions of Play. However, Play 2.4 can read cookies and other data encrypted in both the old and new format.

The crypto transformation is configured in `play.crypto.aes.transformation` and the default value has changed from `AES` to `AES/CTR/NoPadding`, which is more secure.

When you call `Crypto.encryptAES` in Play 2.4 it will use the configured transformation (default `AES/CTR/NoPadding`) to encrypt the data and then encode the result in the new format that supports initialization vectors.

When you call `Crypto.decryptAES` it will decode both the old and new formats. The old format is always decoded using the AES transformation. The new format is decoded using the configured transformation (default `AES/CTR/NoPadding`).

If you wish to continue using the older format of encryption decryption, here is the [link] (https://github.com/playframework/playframework/blob/2.3.6/framework/src/play/src/main/scala/play/api/libs/Crypto.scala#L187-L277) that provides all the necessary information.

## Anorm

Anorm has been pulled out of the core of Play into a separately managed project that can have its own lifecycle.  To add a dependency on it, use:

```scala
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.4.0"
```

The new Anorm version includes various fixes and improvements.

Following [BatchSQL #3016](https://github.com/playframework/playframework/commit/722cd55a3a5369f911f5d11f7c93ba4bf100ca23), `SqlQuery` case class is refactored as a trait with companion object. 
Consequently, `BatchSql` is now created by passed a raw statement which is validated internally.

```scala
import anorm.BatchSql

// Before
BatchSql(SqlQuery("SQL")) // No longer accepted (won't compile)

// Now
BatchSql("SQL")
// Simpler and safer, as SqlQuery is created&validated internally
```

### Parsing

It's now possible to get value from `Row` using column index.

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
SQL("SELECT * FROM Test WHERE cat IN ({categories})").
 on('categories -> List(1, 3, 4)

SQL("SELECT * FROM Test WHERE cat IN ({categories})").
 on('categories -> Set(1, 3, 4)

SQL("SELECT * FROM Test WHERE cat IN ({categories})").
 on('categories -> SortedSet("a", "b", "c")

SQL("SELECT * FROM Test WHERE cat IN ({categories})").
 on('categories -> Stream(1, 3, 4)

SQL("SELECT * FROM Test WHERE cat IN ({categories})").
 on('categories -> Vector("a", "b", "c")
```

**Numeric and boolean types**

Column conversions for basic types like numeric and boolean ones have been improvided.

Some invalid conversions are removed:

Column (JDBC type) | (as) JVM/Scala type
-------------------|---------------------
Double             | Boolean
Int                | Boolean

There are new conversions extending column support.

Column (JDBC type) | (as) JVM/Scala type
-------------------|---------------------
BigDecimal         | BigInteger
BigDecimal         | Int
BigDecimal         | Long
BigInteger         | BigDecimal
BigInteger         | Int
BigInteger         | Long
Boolean            | Int
Boolean            | Long
Boolean            | Short
Byte               | BigDecimal
Float              | BigDecimal
Int                | BigDecimal
Long               | Int
Short              | BigDecimal

**Misc**

- **Binary data**: New column conversions for binary columns (bytes, stream, blob), to be parsed as `Array[Byte]` or `InputStream`.
- **Joda Time**: New conversions for Joda `Instant` or `DateTime`, from `Long`, `Date` or `Timestamp` column.
- Parses text column as `UUID` value: `SQL("SELECT uuid_as_text").as(scalar[UUID].single)`.
- Passing `None` for a nullable parameter is deprecated, and typesafe `Option.empty[T]` must be use instead.

## Specs 2 support

Play's support for specs2 has been pulled out of `play-test`.  Now if you want to use Play's specs2 support, you have to explicitly add a dependency on `play-specs2`, which can be done using the convenient alias provided by the Play sbt plugin:

```scala
libraryDependencies += specs2 % Test
```
