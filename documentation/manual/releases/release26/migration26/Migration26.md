<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.6 Migration Guide

This is a guide for migrating from Play 2.5 to Play 2.6. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.5 Migration Guide|Migration25]].

## How to migrate

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

## Scala ActionBuilder and BodyParser changes:

The Scala `ActionBuilder` trait has been modified to specify the type of the body as a type parameter, and add an abstract `parser` member as the default body parsers. You will need to modify your ActionBuilders and pass the body parser directly.

The `Action` global object and `BodyParsers.parse` are now deprecated. They are replaced by injectable traits, `DefaultActionBuilder` and `PlayBodyParsers` respectively.

To provide a mostly source-compatible API, controllers can extend the `AbstractController` class and pass through the `ControllerComponents` in the constructor:

```
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

See [[JPAMigration26]].

## I18n Migration Notes

See [[MessagesMigration26]].

## Removed Crypto API

The Crypto API has removed the deprecated class `play.api.libs.Crypto` and `play.libs.Crypto` and `AESCTRCrypter`.  The CSRF references to `Crypto` have been replaced by `CSRFTokenSigner`.  The session cookie references to `Crypto` have been replaced with `CookieSigner`.  Please see [[CryptoMigration25]] for more information.

### Removed Yaml API

We removed `play.libs.Yaml` since there was no use of it inside of play anymore.
If you still need support for the Play YAML integration you need to add `snakeyaml` in you `build.sbt`:

```
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
```

And create the following Wrapper in your Code:

```
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

### Guice DI support moved to separate module

In Play 2.6, the core Play module no longer includes Guice. You can add it by adding `guice` to your `libraryDependencies`:

```
libraryDependencies += guice
```

If you explicitly depend on an alternate DI library for play, or have defined your own custom application loader, no changes should be required.

Libraries that provide Play DI support should define the `play.application.loader` configuration key. If no external DI library is provided, Play will refuse to start unless you point that to an `ApplicationLoader`.

### Removed Libraries

In order to make the default play distribution a bit smaller we removed some libraries.
The following libraries are no longer dependencies in Play 2.6, so you will need to manually add them to your build if you use them.

#### Joda-Time removal

If you can, you could migrate all occurences to Java8 `java.time`.

If you can't and still need to use Joda-Time in Play Forms and Play-Json you can just add the `play-joda` project:

```
libraryDependencies += "com.typesafe.play" % "play-joda" % "1.0.0"
```

And then import the corresponding Object for Forms:

```
import play.api.data.JodaForms._
```

or for Play-Json

```
import play.api.data.JodaWrites._
import play.api.data.JodaReads._
```

#### Joda-Convert removal

Play had some internal uses of `joda-convert` if you used it in your project you need to add it to your `build.sbt`:

```
libraryDependencies += "org.joda" % "joda-convert" % "1.8.1"
```

#### XercesImpl removal

For XML handling Play used the Xerces XML Library. Since modern JVM are using Xerces as a reference implementation we removed it.
If your project relies on the external package you can simply add it to your `build.sbt`:

```
libraryDependencies += "xerces" % "xercesImpl" % "2.11.0"
```

#### H2 removal

Prior versions of Play prepackaged the H2 database. But to make the core of Play smaller we removed it.
If you make use of h2 you can add it to your `build.sbt`:

```
libraryDependencies += "com.h2database" % "h2" % "1.4.191"
```

If you only used it in your test you can also just use the `Test` scope:

```
libraryDependencies += "com.h2database" % "h2" % "1.4.191" % Test
```

The [[H2 Browser|Developing-with-the-H2-Database#H2-Browser]] will still work after you added the dependency.

#### snakeyaml removal

Play removed `play.libs.Yaml` and therefore the dependency on `snakeyaml` was dropped.
If you still use it add it to your `build.sbt`:

```
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
```

### Tomcat-servlet-api removal

Play removed the `tomcat-servlet-api` since it was of no use.

```
libraryDependencies += "org.apache.tomcat" % "tomcat-servlet-api" % "8.0.33"
```

### Akka Migration

The deprecated static methods `play.libs.Akka.system` and `play.api.libs.concurrent.Akka.system` were removed.  Please dependency inject an `ActorSystem` instance for access to the actor system.

### Request tags deprecated, replaced with attributes

In Play each `Request` and `RequestHeader` object carries a map of strings called *tags*. This map can be used to attach extra information to a request. In Play 2.6 request tags have been deprecated. A new alternative to tags, called *attributes*, should be used instead.

Unlike tags, which can only be strings, attributes have types. Attributes are identified by a `TypedKey<T>` object that holds the attribute's type. This means the type system can catch errors in your code. It also means you can attach normal objects to a request, not just strings.

In Play 2.6, tags are still provided and still work. However, tags will be removed in a future version of Play so you should update your existing code to use attributes instead.

Existing Java code:

```java
// Tags have string keys
final String USER_ID = "userId";
...
// Store the User object's id in the tags map
User user = getUser(...);
req.tags.put(USER_ID, Long.toString(user.getId()));
...
// Get the user's id out of the tags map then look up the original User object
User user = getUserById(Long.parseLong(req.tags.get(USER_ID)));
```

Updated Java code:

```java
// Use a key with type User
import play.api.libs.typedmap.TypedKey
final TypedKey<User> USER = TypedKeyFactory.create("user");
...
// Create new copy of the request with the USER attribute added
User user = getUser(...);
Request reqWithUser = req.withAttr(USER, user);
...
// Get the USER attribute from the request
User user = req.attr(USER);
```

Existing Scala code:

```scala
// Tags have string keys
val UserId: String = "userId"
...
// Store the User object's id in the tags map
val user: User = getUser(...)
val reqWithUserId = req.copy(tags = req.tags + (UserId -> user.id.toString))
...
// Get the user's id out of the tags map then look up the original User object
User user = getUserById(Long.parseLong(reqWithUserId.tags(UserId)))
```

Updated Scala code:

```scala
// Use a key with type User
import play.api.libs.typedmap.TypedKey
val User: TypedKey[User] = TypedKey("user")
...
// Create new copy of the request with the User attribute added
val user: User = getUser(...)
val reqWithUser = req.withAttr(User, user)
...
// Get the User attribute from the request
val user: User = req.attr(User)
```

#### Request Security username property is now an attribute

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

If you used any of the `Router.Tags.*` tags, you should change your code to use the new `Router.HandlerDefAttr` attribute instead. The existing tags are still available, but are deprecated and will be removed in a future version of Play.

The attribute contains a `HandlerDef` object that contains all the information that is currently in the tags. The relationship between a `HandlerDef` object and its tags is as follows:

```scala
RoutePattern -> handlerDef.path
RouteVerb -> handlerDef.verb
RouteController -> handlerDef.controller
RouteActionMethod -> handlerDef.method
RouteComments -> handlerDef.comments
```

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

``` java
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

The requireJs template helper in [`views/helper/requireJs.scala.html`](https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/views/helper/requireJs.scala.html) used `Play.maybeApplication` to access the configuration.

The requireJs template helper has an extra parameter `isProd` added to it that indicates whether the minified version of the helper should be used:

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


