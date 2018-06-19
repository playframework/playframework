<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.7 Migration Guide

This is a guide for migrating from Play 2.6 to Play 2.7. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.6 Migration Guide|Migration26]].

## `play.allowGlobalApplication` defaults to `false`

`play.allowGlobalApplication = false` is set by default in Play 2.7.0. This means `Play.current` will throw an exception when called. You can set this to `true` to make `Play.current` and other deprecated static helpers work again, but be aware that this feature will be removed in future versions.

In the future, if you still need to use static instances of application components, you can use [static injection](https://github.com/google/guice/wiki/Injections#static-injections) to inject them using Guice, or manually set static fields on startup in your application loader. These approaches should be forward compatible with future versions of Play, as long as you are careful never to run apps concurrently (e.g. in tests).

## Guice compatibility changes

Guice was upgraded to version [4.2.0](https://github.com/google/guice/wiki/Guice42), which causes the following breaking changes:

 - `play.test.TestBrowser.waitUntil` expects a `java.util.function.Function` instead of a `com.google.common.base.Function` now.
 - In Scala, when overriding the `configure()` method of `AbstractModule`, you need to prefix that method with the `override` identifier now (because it's non-abstract now).

## `play.Logger` deprecated

`play.Logger` has been deprecated in favor of using SLF4J directly. You can create an SLF4J logger with `private static final Logger logger = LoggerFactory.getLogger(YourClass.class);`. If you'd like a more concise solution, you may also consider [Project Lombok's `@Slf4j` annotation](https://projectlombok.org/features/log).

If you have a `logger` entry in your logback.xml referencing the `application` logger, you may remove it.

    <logger name="application" level="DEBUG" />

Each logger should have a unique name matching the name of the class where it is used. In this way, you can configure a different log level for each class. You can also set the log level for a given package. For example, to set the log level for all of Play's internal classes to the info level, you can set:

    <logger name="play" level="INFO" />

## Evolutions comment syntax changes

Play Evolutions now correctly supports SQL92 comment syntax. This means you can write evolutions using `--` at the beginning of a line instead of `#` wherever you choose. Newly generated evolutions using the Evolutions API will now also use SQL92-style comment syntax in all areas. Documentation has also been updated accordingly to prefer the SQL92 style, though the older comment style is still fully supported.

## StaticRoutesGenerator removed

The `StaticRoutesGenerator`, which was deprecated in 2.6.0, has been removed. If you are still using it, you will likely have to remove a line like this, so your build compiles:

```scala
routesGenerator := StaticRoutesGenerator
```

Then you should migrate your static controllers to use classes with instance methods.

If you were using the `StaticRoutesGenerator` with dependency-injected controllers, you likely want to remove the `@` prefix from the controller names. The `@` is only needed if you wish to have a new controller instance created on each request using a `Provider`, instead of having a single instance injected into the router.


### `application/javascript` as default content type for JavaScript

`application/javascript` is now the default content-type returned for JavaScript instead of `text/javascript`. For generated `<script>` tags, we are now also omitting the `type` attribute. See more details about omitting `type` attribute at the [HTML 5 specification](https://www.w3.org/TR/html51/semantics-scripting.html#element-attrdef-script-type).  

## `Router#withPrefix` should always add a prefix

Previously, `router.withPrefix(prefix)` was meant to add a prefix to a router, but still allowed "legacy implementations" to update their existing prefix. Play's `SimpleRouter` and other classes followed this behavior. Now all implementations have been updated to add the prefix, so `router.withPrefix(prefix)` should always return a router that routes `s"$prefix/$path"` the same way `router` routes `path`.

By default, routers are unprefixed, so this will only cause a change in behavior if you are calling `withPrefix` on a router that has already been returned by `withPrefix`. To replace a prefix that has already been set on a router, you must call `withPrefix` on the original unprefixed router rather than the prefixed version.

## Play WS Updates

In Play 2.6, we extracted most of Play-WS into a [standalone project](https://github.com/playframework/play-ws) that has an independent release cycle. This project now has a major release that requires some changes in Play itself.

### Scala API

1. `play.api.libs.ws.WSRequest.requestTimeout` now returns an `Option[Duration]` instead of an `Option[Int]`.

### Java API:

1. `play.libs.ws.WSRequest.getUsername` now returns an `Optional<String>` instead of a `String`.
1. `play.libs.ws.WSRequest.getContentType` now returns an `Optional<String>` instead of a `String`.
1. `play.libs.ws.WSRequest.getPassword` now returns an `Optional<String>` instead of a `String`.
1. `play.libs.ws.WSRequest.getScheme` now returns an `Optional<WSScheme` instead of a `WSScheme`.
1. `play.libs.ws.WSRequest.getCalculator` now returns an `Optional<WSSignatureCalculator>` instead of a `WSSignatureCalculator`.
1. `play.libs.ws.WSRequest.getRequestTimeout` now returns an `Optional<Duration>` instead of a `long`.
1. `play.libs.ws.WSRequest.getRequestTimeoutDuration` was removed in favor of using `play.libs.ws.WSRequest.getRequestTimeout`.
1. `play.libs.ws.WSRequest.getFollowRedirects` now returns an `Optional<Boolean>` instead of a `boolean`.

Some new methods were added to improve the Java API too:

New method `play.libs.ws.WSResponse.getBodyAsSource` converts a response body into `Source<ByteString, ?>`. For example:

```java
wsClient.url("https://www.playframework.com")
    .stream() // this returns a CompletionStage<StandaloneWSResponse>
    .thenApply(StandaloneWSResponse::getBodyAsSource);
```

Other methods that were added to improve Java API:

1. `play.libs.ws.WSRequest.getBody` returns the body configured for that request. It can be useful when implementing `play.libs.ws.WSRequestFilter`
1. `play.libs.ws.WSRequest.getMethod` returns the method configured for that request.
1. `play.libs.ws.WSRequest.getAuth` returns the `WSAuth`.
1. `play.libs.ws.WSRequest.setAuth` sets the `WSAuth` for that request.
1. `play.libs.ws.WSResponse.getUri` gets the `URI` for that response. 

## HikariCP update and new configuration

HikariCP was updated to the latest version which finally removed the configuration `initializationFailFast`, replaced by `initializationFailTimeout`. See [HikariCP changelog](https://github.com/brettwooldridge/HikariCP/blob/dev/CHANGES) and [documentation for `initializationFailTimeout`](https://github.com/brettwooldridge/HikariCP#infrequently-used) to better understand how to use this configuration.

## BoneCP removed

BoneCP is removed. If your application is configured to use BoneCP, you need to switch to [HikariCP](http://brettwooldridge.github.io/HikariCP/) which is the default JDBC connection pool.

```
play.db.pool = "default"  # Use the default connection pool provided by the platform (HikariCP)
play.db.pool = "hikaricp" # Use HikariCP

```

You may need to reconfigure the pool to use HikariCP. For example, if you want to configure the maximum number of connections for HikariCP, it would be as follows.

```
play.db.prototype.hikaricp.maximumPoolSize = 15
```

For more details, see [[JDBC configuration section|SettingsJDBC]].

Also, you can use your own pool that implements `play.api.db.ConnectionPool` by specifying the fully-qualified class name.

```
play.db.pool=your.own.ConnectionPool
```

## Java `Http.Context` changed

Request tags, which [[have been deprecated|Migration26#Request-tags-deprecation]] in Play 2.6, have finally been removed in Play 2.7.
Therefore the `args` map of a `Http.Context` instance no longer contains these removed request tags as well.
Instead you can use the `contextObj.request().attrs()` method now, which provides you the equivalent request attributes.

## All Java form `validate` methods need to be migrated to class-level constraints

The "old" `validate` methods of a Java form will not be executed anymore.
Like announced in the [[Play 2.6 Migration Guide|Migration26#Java-Form-Changes]] you have to migrate such `validate` methods to [[class-level constraints|JavaForms#advanced-validation]].

> **Important**: When upgrading to Play 2.7 you will not see any compiler warnings indicating that you have to migrate your `validate` methods (because Play executed them via reflection).

## SecurityHeadersFilter's contentSecurityPolicy deprecated for CSPFilter

The [[SecurityHeaders filter|SecurityHeaders]] has a `contentSecurityPolicy` property: this is deprecated in 2.7.0.  `contentSecurityPolicy` has been changed from `default-src 'self'` to `null` -- the default setting of `null` means that a `Content-Security-Policy` header will not be added to HTTP responses from the SecurityHeaders filter.  Please use the new [[CSPFilter]] to enable CSP functionality.

If `play.filters.headers.contentSecurityPolicy` is not `null`, you will receive a warning.  It is technically possible to have `contentSecurityPolicy` and the new `CSPFilter` active at the same time, but this is not recommended.

You can enable the new `CSPFilter` by adding it to the `play.filters.enabled` property:

```hocon
play.filters.enabled += play.filters.csp.CSPFilter
```

> **NOTE**: You will want to review the Content Security Policy closely to ensure it meets your needs.  The new `CSPFilter` is notably more permissive than `default-src ‘self’`, and is based off the Google Strict CSP configuration.  You can use the `report-only` functionality with a [[CSP report controller|CSPFilter#Configuring-CSP-Report-Only]] to review policy violations.

Please see the documentation in [[CSPFilter]] for more information.

## play.mvc.Results.TODO moved to play.mvc.Controller.TODO

All Play's error pages have been updated to render a CSP nonce if the [[CSP filter|CSPFilter]] is present.  This means that the error page templates must take a request as a parameter.  In 2.6.x, the `TODO` field was previously rendered as a static result instead of an action with an HTTP context, and so may have been called outside the controller.  In 2.7.0, the `TODO` field has been removed, and there is now a `TODO()` method in `play.mvc.Controller` instead:

```java
public abstract class Controller extends Results implements Status, HeaderNames {
    public static Result TODO() {
        play.mvc.Http.Request request = Http.Context.current().request();
        return status(NOT_IMPLEMENTED, views.html.defaultpages.todo.render(request.asScala()));
    }
}
```

## Removed libraries

To make the default play distribution a bit smaller we removed some libraries. The following libraries are no longer dependencies in Play 2.7, so you will need to add them manually to your build if you use them.

### Apache Commons (`commons-lang3` and `commons-codec`)

Play had some internal uses of `commons-codec` and `commons-lang3` if you used it in your project you need to add it to your `build.sbt`:

```scala
libraryDependencies += "commons-codec" % "commons-codec" % "1.10"
```

or:

```scala
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6"
```

## Other libraries updates

This section lists significant updates made to our dependencies.

### `Guava` version updated to 25.1-jre

Play 2.6.x provided 23.0 version of Guava library. Now it is updated to last actual version, 25.1-jre. Lots of changes were made in the library, and you can see the full changelog [here](https://github.com/google/guava/releases).

### specs2 updated to 4.2.0

The previous version was `3.8.x`. There are many changes and improvements, so we recommend that you read [the release notes](https://github.com/etorreborre/specs2/releases) for the recent versions of Specs2. The used version updated the [Mockito](http://site.mockito.org/) version used to `2.18.x`, so we also have updated it.

### Jackson updated to 2.9

Jackson version was updated from 2.8 to 2.9. The release notes for this version are [here](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.9). It is a release that keeps compatibility, so your application should not be affected. But you may be interested in the new features.

### Hibernate Validator updated to 6.0

[Hibernate Validator](http://hibernate.org/validator) was updated to version 6.0 which is now compatible with [Bean Validation](http://beanvalidation.org/) 2.0. See what is new [here](http://hibernate.org/validator/releases/6.0/#whats-new) or read [this detailed blog post](http://in.relation.to/2017/08/07/and-here-comes-hibernate-validator-60/) about the new version.

## Internal changes

Many changes have been made to Play's internal APIs. These APIs are used internally and don't follow a normal deprecation process. Changes may be mentioned below to help those who integrate directly with Play internal APIs.

### `Server.getHandlerFor` has moved to `Server#getHandlerFor`

The `getHandlerFor` method on the `Server` trait was used internally by the Play server code when routing requests. It has been removed and replaced with a method of the same name on the `Server` object.
