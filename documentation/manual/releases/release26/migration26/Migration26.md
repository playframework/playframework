<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.6 Migration Guide

This is a guide for migrating from Play 2.5 to Play 2.6. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.5 Migration Guide|Migration25]].

## How to migrate

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

### Play upgrade

Update the Play version number in project/plugins.sbt to upgrade Play:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.x")
```

Where the "x" in `2.6.x` is the minor version of Play you want to use, per instance `2.6.0`.

### sbt upgrade to 0.13.13

Although Play 2.6 will still work with sbt 0.13.11, we recommend upgrading to the latest sbt version, 0.13.13.  The 0.13.13 release of sbt has a number of [improvements and bug fixes](https://github.com/sbt/sbt/releases/tag/v0.13.13).

Update your `project/build.properties` so that it reads:

```
sbt.version=0.13.13
```

### Guice DI support moved to separate module

In Play 2.6, the core Play module no longer includes Guice. You will need to configure the Guice module by adding `guice` to your `libraryDependencies`:

```scala
libraryDependencies += guice
```

### Play JSON moved to separate project

Play JSON has been moved to a separate library hosted at https://github.com/playframework/play-json. Since Play JSON has no depependencies on the rest of Play, the main change is that the `json` value from `PlayImport` will no longer work in your SBT build. Instead, you'll have to specify the library manually:

```scala
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
```

Also, Play JSON has a separate versioning scheme, so the version no longer is in sync with the Play version.

## Scala ActionBuilder and BodyParser changes:

The Scala `ActionBuilder` trait has been modified to specify the type of the body as a type parameter, and add an abstract `parser` member as the default body parsers. You will need to modify your ActionBuilders and pass the body parser directly.

The `Action` global object and `BodyParsers.parse` are now deprecated. They are replaced by injectable traits, `DefaultActionBuilder` and `PlayBodyParsers` respectively.

To provide a mostly source-compatible API, controllers can extend the `AbstractController` class and pass through the `ControllerComponents` in the constructor:

```scala
class FooController @Inject() (components: ControllerComponents) extends AbstractController(components) {
  // Action and parse now use the injected components
  def foo = Action(parse.text) {
    Ok
  }
}
```

This trait makes `Action` and `parse` refer to injected instances rather than the global objects.

`ControllerComponents` is simply meant to bundle together components typically used in a controller. You may also wish to create your own base controller for your app by extending `BaseController` and injecting your own bundle of components (though Play does not require controllers to implement any particular trait).

## JPA Migration Notes

See [[JPA migration notes|JPAMigration26]].

## I18n Migration Notes

See [[I18N API Migration|MessagesMigration26]].

## Cache APIs Migration Notes

See [[Cache APIs Migration|CacheMigration26]]

## Removed APIs

### Removed Crypto API

The Crypto API has removed the deprecated class `play.api.libs.Crypto` and `play.libs.Crypto` and `AESCTRCrypter`.  The CSRF references to `Crypto` have been replaced by `CSRFTokenSigner`.  The session cookie references to `Crypto` have been replaced with `CookieSigner`.  Please see [[CryptoMigration25]] for more information.

### Removed Yaml API

We removed `play.libs.Yaml` since there was no use of it inside of play anymore.
If you still need support for the Play YAML integration you need to add `snakeyaml` in you `build.sbt`:

```scala
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
```

And create the following Wrapper in your Code:

```java
public class Yaml {

    private final play.Environment environment;

    @Inject
    public Yaml(play.Environment environment) {
        this.environment = environment;
    }

    /**
     * Load a Yaml file from the classpath.
     */
    public Object load(String resourceName) {
        return load(
            environment.resourceAsStream(resourceName),
            environment.classLoader()
        );
    }

    /**
     * Load the specified InputStream as Yaml.
     *
     * @param classloader The classloader to use to instantiate Java objects.
     */
    public Object load(InputStream is, ClassLoader classloader) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml(new CustomClassLoaderConstructor(classloader));
        return yaml.load(is);
    }

}
```

If you explicitly depend on an alternate DI library for play, or have defined your own custom application loader, no changes should be required.

Libraries that provide Play DI support should define the `play.application.loader` configuration key. If no external DI library is provided, Play will refuse to start unless you point that to an `ApplicationLoader`.

### Removed libraries

In order to make the default play distribution a bit smaller we removed some libraries.
The following libraries are no longer dependencies in Play 2.6, so you will need to manually add them to your build if you use them.

#### Joda-Time removal

If you can, you could migrate all occurences to Java8 `java.time`.

If you can't and still need to use Joda-Time in Play Forms and Play-Json you can just add the `play-joda` project:

```scala
libraryDependencies += "com.typesafe.play" % "play-joda" % "1.0.0"
```

And then import the corresponding Object for Forms:

```scala
import play.api.data.JodaForms._
```

or for Play-Json

```scala
import play.api.data.JodaWrites._
import play.api.data.JodaReads._
```

#### Joda-Convert removal

Play had some internal uses of `joda-convert` if you used it in your project you need to add it to your `build.sbt`:

```scala
libraryDependencies += "org.joda" % "joda-convert" % "1.8.1"
```

#### XercesImpl removal

For XML handling Play used the Xerces XML Library. Since modern JVM are using Xerces as a reference implementation we removed it.
If your project relies on the external package you can simply add it to your `build.sbt`:

```scala
libraryDependencies += "xerces" % "xercesImpl" % "2.11.0"
```

#### H2 removal

Prior versions of Play prepackaged the H2 database. But to make the core of Play smaller we removed it.
If you make use of h2 you can add it to your `build.sbt`:

```scala
libraryDependencies += "com.h2database" % "h2" % "1.4.191"
```

If you only used it in your test you can also just use the `Test` scope:

```scala
libraryDependencies += "com.h2database" % "h2" % "1.4.191" % Test
```

The [[H2 Browser|Developing-with-the-H2-Database#H2-Browser]] will still work after you added the dependency.

#### snakeyaml removal

Play removed `play.libs.Yaml` and therefore the dependency on `snakeyaml` was dropped.
If you still use it add it to your `build.sbt`:

```scala
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
```

### Tomcat-servlet-api removal

Play removed the `tomcat-servlet-api` since it was of no use.

```scala
libraryDependencies += "org.apache.tomcat" % "tomcat-servlet-api" % "8.0.33"
```

### Akka Migration

The deprecated static methods `play.libs.Akka.system` and `play.api.libs.concurrent.Akka.system` were removed.  Please dependency inject an `ActorSystem` instance for access to the actor system.

For Scala:

```scala
class MyComponent @Inject() (system: ActorSystem) {

}
```

And for Java:

```java
public class MyComponent {

    private final ActorSystem system;

    @Inject
    public MyComponent(ActorSystem system) {
        this.system = system;
    }
}
```

### Request attributes

All request objects now contain *attributes*. Request attributes are a replacement for request *tags*. Tags have now been deprecated and you should upgrade to attributes. Attributes are more powerful than tags; you can use attributes to store objects in requests, wherease tags only supported storing strings.

#### Request tags deprecation

Tags have been deprecated so you should start migrating from using tags to using attributes. Migration should be fairly straightforward.

The easiest migration path is to migrate from a tag to an attribute with a `String` type.

Java before:

```java
// Getting a tag from a Request or RequestHeader
String userName = req.tags().get("userName");
// Setting a tag on a Request or RequestHeader
req.tags().put("userName", newName);
// Setting a tag with a RequestBuilder
Request builtReq = requestBuilder.tag("userName", newName).build();
```

Java after:

```java
class Attrs {
  public static final TypedKey<String> USER_NAME = TypedKey.<String>create("userName");
}
// Getting an attribute from a Request or RequestHeader
String userName = req.attrs().get(Attrs.USER_NAME);
String userName = req.attrs().getOptional(Attrs.USER_NAME);
// Setting an attribute on a Request or RequestHeader
Request newReq = req.withTags(req.tags().put(Attrs.USER_NAME, newName));
// Setting an attribute with a RequestBuilder
Request builtReq = requestBuilder.attr(Attrs.USER_NAME, newName).build();
```

Scala before:

```scala
// Getting a tag from a Request or RequestHeader
val userName: String = req.tags("userName")
val optUserName: Option[String] = req.tags.get("userName")
// Setting a tag on a Request or RequestHeader
val newReq = req.copy(tags = req.tags.updated("userName", newName))
```

Scala after:

```scala
object Attrs {
  val UserName: TypedKey[String] = TypedKey[String]("userName")
}
// Getting an attribute from a Request or RequestHeader
val userName: String = req.attrs(Attrs.UserName)
val optUserName: [String] = req.attrs.get(Attrs.UserName)
// Setting an attribute on a Request or RequestHeader
val newReq = req.withAttrs(req.attrs.updated(Attrs.UserName, newName))
```

However, if appropriate, we recommend you convert your `String` tags into attributes with non-`String` values. Converting your tags into non-`String` objects has several benefits. First, you will make your code more type-safe. This will increase your code's reliability and make it easier to understand. Second, the objects you store in attributes can contain multiple properties, allowing you to aggregate multiple tags into a single value. Third, converting tags into attributes means you don't need to encode and decode values from `String`s, which may increase performance.

```java
class Attrs {
  public static final TypedKey<User> USER = TypedKey.<User>create("user");
}
```

Scala after:

```scala
object Attrs {
  val UserName: TypedKey[User] = TypedKey[User]("user")
}
```

#### Calling `FakeRequest.withCookies` no longer updates the `Cookies` header

Internally request cookies are now stored in a request attribute. Previously they were stored in the request's `Cookie` header `String`. This required encoding and decoding the cookie to the header whenever the cookie changed.

Now that cookies are stored in request attributes updating the cookie will change the new cookie attribute but not the `Cookie` HTTP header. This will only affect your tests if you're relying on the fact that calling `withCookies` will update the header.

If you still need the old behavior you can still use `Cookies.encodeCookieHeader` to convert the `Cookie` objects into an HTTP header then store the header with `FakeRequest.withHeaders`.

#### play.api.mvc.Security.username (Scala API), session.username config key and dependent actions helpers are deprecated

`Security.username` just retrieves the `session.username` key from configuration, which defined the session key used to get the username. It was removed since it required statics to work, and it's fairly easy to implement the same or similar behavior yourself.

You can read the username session key from configuration yourself using `configuration.get[String]("session.username")`.

If you're using the `Authenticated(String => EssentialAction)` method, you can easily create your own action to do something similar:

```scala
  def AuthenticatedWithUsername(action: String => EssentialAction) =
    WithAuthentication[String](_.session.get(UsernameKey))(action)
```

where `UsernameKey` represents the session key you want to use for the username.

#### Request Security (Java API) username property is now an attribute

The Java Request object contains a `username` property which is set when the `Security.Authenticated` annotation is added to a Java action. In Play 2.6 the username property has been deprecated. The username property methods have been updated to store the username in the `Security.USERNAME` attribute. You should update your code to use the `Security.USERNAME` attribute directly. In a future version of Play we will remove the username property.

The reason for this change is that the username property was provided as a special case for the `Security.Authenticated` annotation. Now that we have attributes we don't need a special case anymore.

Existing Java code:

```java
// Set the username
Request reqWithUsername = req.withUsername("admin");
// Get the username
String username = req1.username();
// Set the username with a builder
Request reqWithUsername = new RequestBuilder().username("admin").build();
```

Updated Java code:

```java
import play.mvc.Security.USERNAME;

// Set the username
Request reqWithUsername = req.withAttr(USERNAME, "admin");
// Get the username
String username = req1.attr(USERNAME);
// Set the username with a builder
Request reqWithUsername = new RequestBuilder().putAttr(USERNAME, "admin").build();
```

#### Router tags are now attributes

If you used any of the `Router.Tags.*` tags, you should change your code to use the new `Router.Attrs.HandlerDef` (Scala) or `Router.Attrs.HANDLER_DEF` (Java) attribute instead. The existing tags are still available, but are deprecated and will be removed in a future version of Play.

This new attribute contains a `HandlerDef` object with all the information that is currently in the tags. The current tags all correspond to a field in the `HandlerDef` object:

| Java tag name         | Scala tag name      | `HandlerDef` method |
|:----------------------|:--------------------|:--------------------|
| `ROUTE_PATTERN`       | `RoutePattern`      | `path`              |
| `ROUTE_VERB`          | `RouteVerb`         | `verb`              |
| `ROUTE_CONTROLLER`    | `RouteController`   | `controller`        |
| `ROUTE_ACTION_METHOD` | `RouteActionMethod` | `method`            |
| `ROUTE_COMMENTS`      | `RouteComments`     | `comments`          |

> **Note**: As part of this change the `HandlerDef` object has been moved from the `play.core.routing` internal package into the `play.api.routing` public API package.

### Remove deprecated `play.Routes`

The deprecated `play.Routes` class used to create a JavaScript router were removed. You now have to use the new Java or Scala helpers:

* [[Javascript Routing in Scala|ScalaJavascriptRouting]]
* [[Javascript Routing in Java|JavaJavascriptRouter]]

### Execution

The `play.api.libs.concurrent.Execution` class has been deprecated, as it was using global mutable state under the hood to pull the "current" application's ExecutionContext.

If you want to specify the implicit behavior that you had previously, then you should pass in the execution context implicitly in the constructor using [[dependency injection|ScalaDependencyInjection]]:

```scala
class MyController @Inject()(implicit ec: ExecutionContext) {

}
```

or from BuiltInComponents if you are using [[compile time dependency injection|ScalaCompileTimeDependencyInjection]]:

```scala
class MyComponentsFromContext(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context) {
  val myComponent: MyComponent = new MyComponent(executionContext)
}
```

However, there are some good reasons why you may not want to import an execution context even in the general case.  In the general case, the application's execution context is good for rendering actions, and executing CPU-bound activities that do not involve blocking API calls or I/O activity.  If you are calling out to a database, or making network calls, then you may want to define your own custom execution context.

The recommended way to create a custom execution context is through `CustomExecutionContext`, which uses the Akka dispatcher system ([java](http://doc.akka.io/docs/akka/current/java/dispatchers.html) / [scala](http://doc.akka.io/docs/akka/current/scala/dispatchers.html))  so that executors can be defined through configuration.

To use your own execution context, extend the `CustomExecutionContext` abstract class with the full path to the dispatcher in the `application.conf` file:

```scala
import play.api.libs.concurrent.CustomExecutionContext

class MyExecutionContext @Inject()(actorSystem: ActorSystem)
 extends CustomExecutionContext(actorSystem, "my.dispatcher.name")
```

```java
import play.libs.concurrent.CustomExecutionContext;
class MyExecutionContext extends CustomExecutionContext {
   @Inject
   public MyExecutionContext(ActorSystem actorSystem) {
     super(actorSystem, "my.dispatcher.name");
   }
}
```

and then inject your custom execution context as appropriate:

```scala
class MyBlockingRepository @Inject()(implicit myExecutionContext: MyExecutionContext) {
   // do things with custom execution context
}
```

Please see [[ThreadPools]] page for more information on custom execution contexts.

## Changes to play.api.test Helpers

The following deprecated test helpers have been removed in 2.6.x:

* `play.api.test.FakeApplication` has been replaced by [`play.api.inject.guice.GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html).
* The `play.api.test.Helpers.route(request)` has been replaced with the `play.api.test.Helpers.routes(app, request)` method.
* The `play.api.test.Helpers.route(request, body)` has been replaced with the [`play.api.test.Helpers.routes(app, request, body)`](api/scala/play/api/test/Helpers$.html) method.

### Java API

* `play.test.FakeRequest` has been replaced by [`RequestBuilder`](api/java/play/mvc/Http.RequestBuilder.html)
* `play.test.FakeApplication` has been replaced with `play.inject.guice.GuiceApplicationBuilder`.  You can create a new `Application` from [`play.test.Helpers.fakeApplication`](api/java/play/inject/guice/GuiceApplicationBuilder.html).
* In `play.test.WithApplication`, the deprecated `provideFakeApplication` method has been removed -- the `provideApplication` method should be used.


## Changes to Template Helpers

The `requireJs` template helper in [`views/helper/requireJs.scala.html`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/views/helper/requireJs.scala.html) used `Play.maybeApplication` to access the configuration.

The `requireJs` template helper has an extra parameter `isProd` added to it that indicates whether the minified version of the helper should be used:

```
@requireJs(core = routes.Assets.at("javascripts/require.js").url, module = routes.Assets.at("javascripts/main").url, isProd = true)
```

## Changes to File Extension to MIME Type Mapping

The mapping of file extensions to MIME types has been moved to `reference.conf` so it is covered entirely through configuration, under `play.http.fileMimeTypes` setting.  Previously the list was hardcoded under `play.api.libs.MimeTypes`.

Note that `play.http.fileMimeTypes` configuration setting is defined using triple quotes as a single string -- this is because several file extensions have syntax that breaks HOCON, such as `c++`.

To append a custom MIME type, use [HOCON string value concatenation](https://github.com/typesafehub/config/blob/master/HOCON.md#string-value-concatenation):

```
play.http.fileMimeTypes = ${play.http.fileMimeTypes} """
  foo=text/bar
"""
```

There is a syntax that allows configurations defined as `mimetype.foo=text/bar` for additional MIME types.  This is deprecated, and you are encouraged to use the above configuration.

### Java API

There is a `Http.Context.current().fileMimeTypes()` method that is provided under the hood to `Results.sendFile` and other methods that look up content types from file extensions.  No migration is necessary.

### Scala API

The `play.api.libs.MimeTypes` class has been changed to `play.api.http.FileMimeTypes` interface, and the implementation has changed to `play.api.http.DefaultMimeTypes`.

All the results that send files or resources now take `FileMimeTypes` implicitly, i.e.

```scala
implicit val fileMimeTypes: FileMimeTypes = ...
Ok(file) // <-- takes implicit FileMimeTypes
```

An implicit instance of `FileMimeTypes` is provided by `AbstractController` through the `ControllerComponents` class, to provide a convenient binding:

```scala
class SendFileController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action { implicit request =>
     val file = readFile()
     Ok(file)  // <-- takes implicit FileMimeTypes
  }
}
```

You can also get a fully configured `FileMimeTypes` instance directly in a unit test:

```scala
val httpConfiguration = new HttpConfigurationProvider(Configuration.load(Environment.simple)).get
val fileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get
```

Or get a custom one:

```scala
val fileMimeTypes = new DefaultFileMimeTypesProvider(FileMimeTypesConfiguration(Map("foo" -> "text/bar"))).get
```
## Updated libraries

### Fluentlenium
The Fluentlenium library was updated to version 3.1.1 and as a result the underlying Selenium version changed to [3.0.1](https://seleniumhq.wordpress.com/2016/10/13/selenium-3-0-out-now/). If you were using Selenium's WebDriver API before, there shouldn't be anything to do. Please check [this](https://seleniumhq.wordpress.com/2016/10/04/selenium-3-is-coming/) announcement for further information.
If you were using the Fluentlenium library you might have to change some syntax to get your tests working again. Please see Fluentlenium's [Migration Guide](http://fluentlenium.org/migration/from-0.13.2-to-1.0-or-3.0/)

## Other Configuration changes

There are some configurations.  The old configuration paths will generally still work, but a deprecation warning will be output at runtime if you use them.  Here is a summary of the changed keys:

| Old key                   | New key                            |
| ------------------------- | ---------------------------------- |
| `play.crypto.secret`      | `play.http.secret.key`             |
| `play.crypto.provider`    | `play.http.secret.provider`        |
