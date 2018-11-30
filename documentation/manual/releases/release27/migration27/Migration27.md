<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.7 Migration Guide

This is a guide for migrating from Play 2.6 to Play 2.7. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.6 Migration Guide|Migration26]].

## How to migrate

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

### Play upgrade

Update the Play version number in `project/plugins.sbt` to upgrade Play:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.x")
```

Where the "x" in `2.7.x` is the minor version of Play you want to use, for instance `2.7.0`.

### sbt upgrade to 1.1.6

Although Play 2.7 still supports sbt 0.13 series, we recommend that you use sbt 1 from now. This new version is actively maintained and supported. To update, change your `project/build.properties` so that it reads:

```
sbt.version=1.1.6
```

At the time of this writing `1.1.6` is the latest version in the sbt 1 family, you may be able to use newer versions too. Check for details in the release notes of your minor version of Play 2.7.x. More information at the list of [sbt releases](https://github.com/sbt/sbt/releases).

## Deprecated APIs were removed

Many deprecated APIs were removed in Play 2.7. If you are still using them, we recommend migrating to the new APIs before upgrading to Play 2.7. Both Javadocs and Scaladocs usually have proper documentation on how to migrate.

## `play.allowGlobalApplication` defaults to `false`

`play.allowGlobalApplication = false` is set by default in Play 2.7.0. This means `Play.current` will throw an exception when called. You can set this to `true` to make `Play.current` and other deprecated static helpers work again, but be aware that this feature will be removed in future versions.

In the future, if you still need to use static instances of application components, you can use [static injection](https://github.com/google/guice/wiki/Injections#static-injections) to inject them using Guice, or manually set static fields on startup in your application loader. These approaches should be forward compatible with future versions of Play, as long as you are careful never to run apps concurrently (e.g., in tests).

Since `Play.current` is still called by some deprecated APIs, when using such APIs, you need to add the following line to your `application.conf` file:

```hocon
play.allowGlobalApplication = true
```

For example, when using `play.api.mvc.Action` object with embedded Play and [[Scala Sird Router|ScalaSirdRouter]], it access the global state:

```scala
import play.api.mvc._
import play.api.routing.sird._
import play.core.server._

// It can also be NettyServer
val server = AkkaHttpServer.fromRouter() {
  // `Action` in this case is the `Action` object which access global state
  case GET(p"/") => Action {
    Results.Ok(s"Hello World")
  }
}
```

The example above either needs you to configure `play.allowGlobalApplication = true` as explained before, or to be rewritten to:

```scala
import play.api._
import play.api.mvc._
import play.api.routing.sird._
import play.core.server._

// It can also be NettyServer
val server = AkkaHttpServer.fromRouterWithComponents() { components: BuiltInComponents => {
    case GET(p"/") => components.defaultActionBuilder {
      Results.Ok(s"Hello World")
    }
  }
}
```

## BodyParsers API consistency

The API for body parser was mixing `Integer` and `Long` to define buffer lengths which could lead to overflow of values. The configuration is now uniformed to use `Long`. It means that if you are depending on `play.api.mvc.PlayBodyParsers.DefaultMaxTextLength` for example, you then need to use a `Long`. As such, `play.api.http.ParserConfiguration.maxMemoryBuffer` is now a `Long` too.

## Guice compatibility changes

Guice was upgraded to version [4.2.2](https://github.com/google/guice/wiki/Guice422) (also see [4.2.1](https://github.com/google/guice/wiki/Guice421) and [4.2.0 release notes](https://github.com/google/guice/wiki/Guice42)), which causes the following breaking changes:

 - `play.test.TestBrowser.waitUntil` expects a `java.util.function.Function` instead of a `com.google.common.base.Function` now.
 - In Scala, when overriding the `configure()` method of `AbstractModule`, you need to prefix that method with the `override` identifier now (because it's non-abstract now).

## Static `Logger` singletons deprecated

Most `static` methods of the Java `play.Logger` and almost all methods of the Scala `play.api.Logger` singleton object have been deprecated. These singletons wrote to the `application` logger, which is referenced in `logback.xml` as:

    <logger name="application" level="DEBUG" />

If you are concerned about changing your logging configuration, the simplest migration here is to define your own singleton "application" logger using `Logger("application")` (Scala) or `Logger.of("application")` (Java). All logs sent to this logger will work exactly like the Play singleton logger. While we don't recommend this approach in general, it's ultimately up to you. Play and Logback do not force you to use any specific naming scheme for your loggers.

If you are comfortable making some straightforward code changes and changing your logging configuration, we instead recommend you create a new logger for each class, with a name matching the class name. This allows you to configure different log levels for each class or package. For example, to set the log level for all `com.example.models` to the info level, you can set in `logback.xml`:

    <logger name="com.example.models" level="INFO" />

To define the logger in each class, you can define:

Java
: ```java
import play.Logger;
private static final Logger.ALogger logger = Logger.of(YourClass.class);
```

Scala
: ```scala
import play.api.Logger
private val logger = Logger(YourClass.class)
```

For Scala, Play also provides a `play.api.Logging` trait that can be mixed into a class or trait to add the `val logger: Logger` automatically:

```scala
import play.api.Logging

class MyClass extends Logging {
  // `logger` is automaticaly defined by the `Logging` trait:
  logger.info("hello!")
}
```

Of course you can also just use [SLF4J](https://www.slf4j.org/) directly:

Java
: ```java
private static final Logger logger = LoggerFactory.getLogger(YourClass.class);
```

Scala
: ```scala
private val logger = LoggerFactory.getLogger(YourClass.class);
```

If you'd like a more concise solution when using SLF4J directly for Java, you may also consider [Project Lombok's `@Slf4j` annotation](https://projectlombok.org/features/log).

> **NOTE**: `org.slf4j.Logger`, the logging interface of SLF4J, does [not yet](https://jira.qos.ch/browse/SLF4J-371) provide logging methods which accept lambda expression as parameters for lazy evaluation. `play.Logger` and `play.api.Logger`, which are mostly simple wrappers for `org.slf4j.Logger`, provide such methods however.

Once you have migrated away from using the `application` logger, you can remove the `logger` entry in your `logback.xml` referencing it:

    <logger name="application" level="DEBUG" />

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

In Play 2.6, we extracted most of Play-WS into a [standalone project](https://github.com/playframework/play-ws) that has an independent release cycle. Play-WS now has a significant release that requires some changes in Play itself.

## Run Hooks

`RunHook.afterStarted()` no longer takes an `InetSocketAddress` as a parameter.

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

### HikariCP will not fail fast

Play 2.7 changes the default value for HikariCP's `initializationFailTimeout` to `-1`. That means your application will start even if the database is not available. You can revert to the old behavior by configuring `initializationFailTimeout` to `1` which will make the pool to fail fast.

If the application is using database [[Evolutions]], then a connection is requested at application startup to verify if there are new evolutions to apply. So this will make the startup fail if the database is not available since a connection is being required. The timeout then will be defined by `connectionTimeout` (default to 30 seconds).

See more details at [[SettingsJDBC]].

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

## Application Loader API changes

If you are using a custom `ApplicationLoader` there is a chance you are manually creating instances of this loader when running the tests. To do that, you first need to create an instance of `ApplicationLoader.Context`, for example:

```scala
val env = Environment.simple()
val context = ApplicationLoader.Context(
  environment = env,
  sourceMapper = None,
  webCommands = new DefaultWebCommands(),
  initialConfiguration = Configuration.load(env),
  lifecycle = new DefaultApplicationLifecycle()
)
val loader = new MyApplicationLoader()
val application = loader.load(context)
```

But the `ApplicationLoader.Context` apply method used in the code above is now deprecated and throws an exception when `webCommands` is not null. The new code should be:

```scala
val env = Environment.simple()
val context = ApplicationLoader.Context.create(env)
val loader = new GreetingApplicationLoader()
val application = loader.load(context)
```

## JPA removals and deprecations

The class `play.db.jpa.JPA`, which has been deprecated in Play 2.6 already, has finally been removed. Have a look at the [[Play 2.6 JPA Migration notes|JPAMigration26]] if you haven't yet.

With this Play release even more JPA related methods and annotations have been deprecated:

* `@play.db.jpa.Transactional`
* `play.db.jpa.JPAApi.em()`
* `play.db.jpa.JPAApi.withTransaction(final Runnable block)`
* `play.db.jpa.JPAApi.withTransaction(Supplier<T> block)`
* `play.db.jpa.JPAApi.withTransaction(String name, boolean readOnly, Supplier<T> block)`

Like already mentioned in the Play 2.6 JPA migration notes, please use a `JPAApi` injected instance as described in [[Using play.db.jpa.JPAApi|JavaJPA#Using-play.db.jpa.JPAApi]] instead of these deprecated methods and annotations.

## Java `Http.Context` changes

See changes made in `play.mvc.Http.Context` APIs. This is only relevant for Java users: [[Java `Http.Context` changes|JavaHttpContextMigration27]].

## All Java form `validate` methods need to be migrated to class-level constraints

The "old" `validate` methods of a Java form will not be executed anymore.
Like announced in the [[Play 2.6 Migration Guide|Migration26#Java-Form-Changes]] you have to migrate such `validate` methods to [[class-level constraints|JavaForms#advanced-validation]].

> **Important**: When upgrading to Play 2.7 you will not see any compiler warnings indicating that you have to migrate your `validate` methods (because Play executed them via reflection).

## Java `Form`, `DynamicForm` and `FormFactory` constructors changed

Constructors of the `Form`, `DynamicForm` and `FormFactory` classes (inside `play.data`) that were using a [`Validator`](https://docs.jboss.org/hibernate/stable/beanvalidation/api/javax/validation/Validator.html) param use a [`ValidatorFactory`](https://docs.jboss.org/hibernate/stable/beanvalidation/api/javax/validation/ValidatorFactory.html) param instead now.
In addition to that, these constructors now also need a [`com.typesafe.config.Config`](https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html) param.
E.g. `new Form(..., validator)` becomes `new Form(..., validatorFactory, config)` now.
This change only effects you if you use the constructors to instantiate a form instead of just using `formFactory.form(SomeForm.class)` - most likely in tests.

## The Java Cache API `get` method has been deprecated in favor of `getOptional`

The `getOptional` methods of the Java `cacheApi` return their results wrapped in an `Optional`.

Changes in `play.cache.SyncCacheApi`:

| **deprecated method**                      | **new method**
|------------------------------------------|----------------------------------------------------
| `<T> T get(String key)`                  | `<T> Optional<T> getOptional(String key)`

Changes in `play.cache.AsyncCacheApi`:

| **deprecated method**                      | **new method**
|------------------------------------------|----------------------------------------------------
| `<T> CompletionStage<T> get(String key)` | `<T> CompletionStage<Optional<T>> getOptional(String key)`

## SecurityHeadersFilter's contentSecurityPolicy deprecated for CSPFilter

The [[SecurityHeaders filter|SecurityHeaders]] has a `contentSecurityPolicy` property: this is deprecated in 2.7.0.  `contentSecurityPolicy` has been changed from `default-src 'self'` to `null` -- the default setting of `null` means that a `Content-Security-Policy` header will not be added to HTTP responses from the SecurityHeaders filter.  Please use the new [[CSPFilter|CspFilter]] to enable CSP functionality.

If `play.filters.headers.contentSecurityPolicy` is not `null`, you will receive a warning.  It is technically possible to have `contentSecurityPolicy` and the new `CSPFilter` active at the same time, but this is not recommended.

You can enable the new `CSPFilter` by adding it to the `play.filters.enabled` property:

```hocon
play.filters.enabled += play.filters.csp.CSPFilter
```

> **NOTE**: You will want to review the Content Security Policy closely to ensure it meets your needs.  The new `CSPFilter` is notably more permissive than `default-src ‘self’`, and is based off the Google Strict CSP configuration.  You can use the `report-only` functionality with a [[CSP report controller|CspFilter#Configuring-CSP-Report-Only]] to review policy violations.

Please see the documentation in [[CSPFilter|CspFilter]] for more information.

## SameSite attribute for CSRF and language cookie

With Play 2.6 the `SameSite` cookie attribute [[was enabled|Migration26#SameSite-attribute,-enabled-for-session-and-flash]] for session and flash by default.
The same is true for the CSRF and the language cookie starting with Play 2.7. By default, the `SameSite` attribute of the CSRF cookie will have the same value like the session cookie has and the language cookie will use `SameSite=Lax` by default.
You can tweak this using configuration. For example:

```
play.filters.csrf.cookie.sameSite = null // no same-site for csrf cookie
play.i18n.langCookieSameSite = "strict" // strict same-site for language cookie
```

## play.mvc.Results.TODO moved to play.mvc.Controller.TODO

All Play's error pages have been updated to render a CSP nonce if the [[CSPFilter|CspFilter]] is present.  This means that the error page templates must take a request as a parameter.  In 2.6.x, the `TODO` field was previously rendered as a static result instead of an action with an HTTP context, and so may have been called outside the controller.  In 2.7.0, the `TODO` field has been removed, and there is now a `TODO(Http.Request request)` method in `play.mvc.Controller` instead:

```java
public abstract class Controller extends Results implements Status, HeaderNames {
    public static Result TODO(play.mvc.Http.Request request) {
        return status(NOT_IMPLEMENTED, views.html.defaultpages.todo.render(request.asScala()));
    }
}
```

## Java DI-agnostic Play `Module` API support added and all built-in Java `Module`s type changed

You can now create DI-agnostic Play `Module` with Java by extending `play.inject.Module`, which is more Java friendly as it is using Java APIs and coded in Java as well. Besides, all the existing built-in Java `Module`s, for example, `play.inject.BuiltInModule` and `play.libs.ws.ahc.AhcWSModule`, are no longer extending Scala `play.api.inject.Module` but Java `play.inject.Module`.

Since Java `play.inject.Module` is a subclass of Scala `play.api.inject.Module`, the `Module` instances can still be used in the same way, except the interface is a little different:

```java
public class MyModule extends play.inject.Module {
    @Override
    public java.util.List<play.inject.Binding<?>> bindings(final play.Environment environment, final com.typesafe.config.Config config) {
        return java.util.Collections.singletonList(
            // Note: it is bindClass() but not bind()
            bindClass(MyApi.class).toProvider(MyApiProvider.class)
        );
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

### `Guava` version updated to 27.0-jre

Play 2.6.x provided 23.0 version of Guava library. Now it is updated to last actual version, 27.0-jre. Lots of changes were made in the library, and you can see the full changelog [here](https://github.com/google/guava/releases).

### specs2 updated to 4.2.0

The previous version was `3.8.x`. There are many changes and improvements, so we recommend that you read [the release notes](https://github.com/etorreborre/specs2/releases) for the recent versions of Specs2. The used version updated the [Mockito](https://site.mockito.org/) version used to `2.18.x`, so we also have updated it.

### Jackson updated to 2.9

Jackson version was updated from 2.8 to 2.9. The release notes for this version are [here](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.9). It is a release that keeps compatibility, so your application should not be affected. But you may be interested in the new features.

### Hibernate Validator updated to 6.0

[Hibernate Validator](http://hibernate.org/validator/) was updated to version 6.0 which is now compatible with [Bean Validation](https://beanvalidation.org/) 2.0. See what is new [here](http://hibernate.org/validator/releases/6.0/#whats-new) or read [this detailed blog post](http://in.relation.to/2017/08/07/and-here-comes-hibernate-validator-60/) about the new version.

> **Note**: Keep in mind that this version may not be fully compatible with other Hibernate dependencies you may have in your project. For example, if you are using [hibernate-jpamodelgen](https://mvnrepository.com/artifact/org.hibernate/hibernate-jpamodelgen) it is required that you use the latest version to ensure everything will work together:
>
> ```scala
> libraryDependencies += "org.hibernate" % "hibernate-jpamodelgen" % "5.3.6.Final" % "provided"
> ```

## Internal changes

Many changes have been made to Play's internal APIs. These APIs are used internally and don't follow a normal deprecation process. Changes may be mentioned below to help those who integrate directly with Play internal APIs.

### `Server.getHandlerFor` has moved to `Server#getHandlerFor`

The `getHandlerFor` method on the `Server` trait was used internally by the Play server code when routing requests. It has been removed and replaced with a method of the same name on the `Server` object.

## CoordinatedShutdown `play.akka.run-cs-from-phase` configuration

The configuration `akka.coordinated-shutdown.exit-jvm` is not supported anymore. When that setting is enabled Play will not start, and an error will be logged. Play ships with default values for `akka.coordinated-shutdown.*` which should be suitable for most scenarios so it's unlikely you'll need to override them. 

The configuration `play.akka.run-cs-from-phase` is not supported anymore and adding it does not affect the application shutdown. A warning is logged if it is present. Play now runs all the phases to ensure that all hooks registered in `ApplicationLifecycle` and all the tasks added to coordinated shutdown are executed. If you need to run `CoordinatedShutdown` from a specific phase, you can always do it manually:

```scala
import akka.actor.ActorSystem
import javax.inject.Inject

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason

class Shutdown @Inject()(actorSystem: ActorSystem) {

  // Define your own reason to run the shutdown
  case object CustomShutdownReason extends Reason

  def shutdown() = {
    // Use a phase that is appropriated for your application
    val runFromPhase = Some(CoordinatedShutdown.PhaseBeforeClusterShutdown)
    val coordinatedShutdown = CoordinatedShutdown(actorSystem).run(CustomShutdownReason, runFromPhase)
  }
}
```

And for Java:

```java
import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;

import javax.inject.Inject;
import java.util.Optional;

class Shutdown {

    public static final CoordinatedShutdown.Reason customShutdownReason = new CustomShutdownReason();

    private final ActorSystem actorSystem;

    @Inject
    public Shutdown(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    public void shutdown() {
        // Use a phase that is appropriated for your application
        Optional<String> runFromPhase = Optional.of(CoordinatedShutdown.PhaseBeforeClusterShutdown());
        CoordinatedShutdown.get(actorSystem).run(customShutdownReason, runFromPhase);
    }

    public static class CustomShutdownReason implements CoordinatedShutdown.Reason {}
}
```

## Change in self-signed HTTPS certificate

It is now generated under `target/dev-mode/generated.keystore` instead of directly on the root folder.

## Application Secret is checked for minimum length

The [[application secret|ApplicationSecret]] configuration `play.http.secret.key` is checked for a minimum length in production.  If the key is fifteen characters or fewer, a warning will be logged.  If the key is eight characters or fewer, then an error is thrown and the configuration is invalid.  You can resolve this error by setting the secret to at least 32 bytes of completely random input, such as `head -c 32 /dev/urandom | base64` or by the application secret generator, using `playGenerateSecret` or `playUpdateSecret`.

The [[application secret|ApplicationSecret]] is used as the key for ensuring that a Play session cookie is valid, i.e. has been generated by the server as opposed to spoofed by an attacker.  However, the secret only specifies a string, and does not determine the amount of entropy in that string.  Anyhow, it is possible to put an upper bound on the amount of entropy in the secret simply by measuring how short it is: if the secret is eight characters long, that is at most 64 bits of entropy, which is insufficient by modern standards.

## Change in default character set on "text/plain" Content Types

The Text and Tolerant Text body parsers now use "US-ASCII" as the default charset, replacing the previous default of `ISO-8859-1`.

This is because of some newer HTTP standards, specifically [RFC 7231, appendix B](https://tools.ietf.org/html/rfc7231#appendix-B), which states "The default charset of ISO-8859-1 for text media types has been removed; the default is now whatever the media type definition says."  The `text/plain` media type definition is defined by [RFC 6657, section 4](https://tools.ietf.org/html/rfc6657#section-4), which specifies US-ASCII.  The Text and Tolerant Text Body parsers use `text/plain` as the content type, so now default appropriately.
