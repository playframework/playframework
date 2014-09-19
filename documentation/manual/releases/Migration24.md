<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Play 2.4 Migration Guide

This guide is for migrating to Play 2.4 from Play 2.3. To migrate to Play 2.3, first follow the [[Play 2.3 Migration Guide|Migration23]].

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

The default is to use the static routes generator.  You must use this if you are not ready to migrate all of your Java actions to be non static methods, or your Scala actions to to be classes.  In most cases, this is quite straight forward to do, in Java it requires deleting the `static` keyword, in Scala it requires changing the word `object` to `class`.  The static router still supports the `@` operator, which will tell it to look up the action from a runtime `Injector`, you may find this useful if you are in a transitional period where some of your actions are static and some are injected.

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