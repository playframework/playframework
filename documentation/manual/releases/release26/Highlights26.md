<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# What's new in Play 2.6

This page highlights the new features of Play 2.6. If you want to learn about the changes you need to make when you migrate to Play 2.6, check out the [[Play 2.6 Migration Guide|Migration26]].

## Scala 2.12 support

Play 2.6 is the first release of Play to have been cross built against Scala 2.12 and 2.11. A number of dependencies were updated so that we can have support for both versions.

You can select which version of Scala you would like to use by setting the `scalaVersion` setting in your `build.sbt`.

For Scala 2.12:

```scala
scalaVersion := "2.12.6"
```

For Scala 2.11:

```scala
scalaVersion := "2.11.12"
```

## PlayService sbt plugin (experimental)

As of Play 2.6.8, Play also offers a `PlayService` plugin. This is a much more minimal Play configuration oriented towards microservices. It uses the standard Maven layout instead of the traditional Play layout, and does not include twirl templates or the sbt-web functionality. For example:

```scala
lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .enablePlugins(RoutesCompiler) // place routes in src/main/resources, or remove if using SIRD/RoutingDsl
  .settings(
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      guice, // remove if not using Play's Guice loader
      akkaHttpServer, // or use nettyServer for Netty
      logback // add Play logging support
    )
  )
```

**Note**: this plugin is considered *experimental*, which means the API may change. We expect it to be stable in Play 2.7.0.

## "Global-State-Free" Applications

The biggest under the hood change is that Play no longer relies on global state.  You can still access the global application through `play.api.Play.current` / `play.Play.application()` in Play 2.6, but it is deprecated.  This sets the stage for Play 3.0, where there is no global state at all.

You can disable access to global application entirely by setting the following configuration value:

```
play.allowGlobalApplication=false
```

The above setting will cause an exception on any invocation of `Play.current`.

## Akka HTTP Server Backend

Play now uses the [Akka-HTTP](https://doc.akka.io/docs/akka-http/current/?language=scala) server engine as the default backend.  More detail about Play's integration with Akka-HTTP can be found [[on the Akka HTTP Server page|AkkaHttpServer]].  There is an additional page on [[configuring Akka HTTP|SettingsAkkaHttp]].

The Netty backend is still available, and has been upgraded to use Netty 4.1.  You can explicitly configure your project to use Netty [[on the NettyServer page|NettyServer]].

## HTTP/2 support (experimental)

Play now has HTTP/2 support on the Akka HTTP server using the `PlayAkkaHttp2Support` module:

```
lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayAkkaHttp2Support)
```

This automates most of the process of setting up HTTP/2. However, it does not work with the `run` command by default. See the [[Akka HTTP Server page|AkkaHttpServer]] for more details.

## Request attributes

Requests in Play 2.6 now contain *attributes*. Attributes allow you to store extra information inside request objects. For example, you can write a filter that sets an attribute in the request and then access the attribute value later from within your actions.

Attributes are stored in a `TypedMap` that is attached to each request. `TypedMap`s are immutable maps that store type-safe keys and values. Attributes are indexed by a key and the key's type indicates the type of the attribute.

Java:
```java
// Create a TypedKey to store a User object
class Attrs {
  public static final TypedKey<User> USER = TypedKey.<User>create("user");
}

// Get the User object from the request
User user = req.attrs().get(Attrs.USER);
// Put a User object into the request
Request newReq = req.addAttr(Attrs.USER, newUser);
```

Scala:
```scala
// Create a TypedKey to store a User object
object Attrs {
  val User: TypedKey[User] = TypedKey.apply[User]("user")
}

// Get the User object from the request
val user: User = req.attrs(Attrs.User)
// Put a User object into the request
val newReq = req.addAttr(Attrs.User, newUser)
```

Attributes are stored in a `TypedMap`. You can read more about attributes in the `TypedMap` documentation: [Javadoc](api/java/play/libs/typedmap/TypedMap.html), [Scaladoc](api/scala/play/api/libs/typedmap/TypedMap.html).

Request tags have now been deprecated and you should migrate to use attributes instead. See the [[tags section|Migration26#Request-tags-deprecation]] in the migration docs for more information.

## Route modifier tags

The routes file syntax now allows you to add "modifiers" to each route that provide custom behavior. We have implemented one such tag in the CSRF filter, the "nocsrf" tag. By default, the following route will not have the CSRF filter applied.

```
+ nocsrf # Don't CSRF protect this route
POST /api/foo/bar ApiController.foobar
```

You can also create your own modifiers: the `+` symbol can be followed by any number of whitespace-separated tags.

These are made available in the `HandlerDef` request attribute (which also contains other metadata on the handler definition in the routes file):

Java:
```java
import java.util.List;
import play.routing.HandlerDef;
import play.routing.Router;

HandlerDef handler = req.attrs().get(Router.Attrs.HANDLER_DEF);
List<String> modifiers = handler.getModifiers();
```

Scala:
```scala
import play.api.routing.{ HandlerDef, Router }
import play.api.mvc.RequestHeader

val handler = request.attrs(Router.Attrs.HandlerDef)
val modifiers = handler.modifiers
```

## Injectable Twirl Templates

Twirl templates can now be created with a constructor annotation using `@this`.  The constructor annotation means that Twirl templates can be injected into templates directly and can manage their own dependencies, rather than the controller having to manage dependencies not only for itself, but also for the templates it has to render.

As an example, suppose a template has a dependency on a component `TemplateRenderingComponent`, which is not used by the controller.

First create a file `IndexTemplate.scala.html` using the `@this` syntax for the constructor. Note that the constructor must be placed **before** the `@()` syntax used for the template's parameters for the `apply` method:

```scala
@this(trc: TemplateRenderingComponent)
@(item: Item)

@{trc.render(item)}
```

By default all generated Scala template classes Twirl creates with the `@this` syntax within Play will automatically be annotated with `@javax.inject.Inject()`. If desired you can change this behavior in `build.sbt`:

```scala
// Add one or more annotation(s):
TwirlKeys.constructorAnnotations += "@java.lang.Deprecated()"

// Or completely replace the default one with your own annotation(s):
TwirlKeys.constructorAnnotations := Seq("@com.google.inject.Inject()")
```

Now define the controller by injecting the template in the constructor:

Java:
```java
public class MyController extends Controller {

  private final views.html.indexTemplate template;

  @Inject
  public MyController(views.html.indexTemplate template) {
    this.template = template;
  }

  public Result index() {
    return ok(template.render());
  }

}
```

Scala:
```scala
class MyController @Inject()(indexTemplate: views.html.IndexTemplate,
                              cc: ControllerComponents)
  extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(indexTemplate())
  }
}
```

Once the template is defined with its dependencies, then the controller can have the template injected into the controller, but the controller does not see `TemplateRenderingComponent`.

## Filters Enhancements

Play now comes with a default set of enabled filters, defined through configuration.  This provides a "secure by default" experience for new Play applications, and tightens security on existing Play applications.

The following filters are enabled by default:

* `play.filters.csrf.CSRFFilter` prevents CSRF attacks, see [[ScalaCsrf]] / [[JavaCsrf]]
* `play.filters.headers.SecurityHeadersFilter` prevents XSS and frame origin attacks, see [[SecurityHeaders]]
* `play.filters.hosts.AllowedHostsFilter` prevents DNS rebinding attacks, see [[AllowedHostsFilter]]

In addition, filters can now be configured through `application.conf`.  To append to the defaults list, use the `+=`:

```
play.filters.enabled+=MyFilter
```

If you want to specifically disable a filter for testing, you can also do that from configuration:

```
play.filters.disabled+=MyFilter
```

Please see [[the Filters page|Filters]] for more details.

> **NOTE**: If you are migrating from an existing project that does not use CSRF form helpers such as `CSRF.formField`, then you may see "403 Forbidden" on PUT and POST requests, from the CSRF filter.  To check this behavior, please add `<logger name="play.filters.csrf" value="TRACE"/>` to your `logback.xml`.  Likewise, if you are running a Play application on something other than localhost, you must configure the [[AllowedHostsFilter]] to specifically allow the hostname/ip you are connecting from.

### gzip filter

If you have the gzip filter enabled you can now also control which responses are and aren't gzipped based on their content types via `application.conf` (instead of writing you own `Filters` class):

```
play.filters.gzip {

    contentType {

        # If non empty, then a response will only be compressed if its content type is in this list.
        whiteList = [ "text/*", "application/javascript", "application/json" ]

        # The black list is only used if the white list is empty.
        # Compress all responses except the ones whose content type is in this list.
        blackList = []
    }
}
```

Please see [[the gzip filter page|GzipEncoding]] for more details.

## JWT Cookies

Play now uses [JSON Web Token](https://tools.ietf.org/html/rfc7519) (JWT) format for session and flash cookies.  This allows for a standardized signed cookie data format, cookie expiration (making replay attacks harder) and more flexibility in signing cookies.

Please see [[Scala|ScalaSessionFlash]] or [[Java|JavaSessionFlash]] pages for more details.

## Logging Marker API

 SLF4J Marker support has been added to [`play.Logger`](api/java/play/Logger.html) and [`play.api.Logger`](api/scala/play/api/Logger.html).

In the Java API, it is a straight port of the SLF4J Logger API.  This is useful, but you may find an SLF4J wrapper like [Godaddy Logger](https://github.com/godaddy/godaddy-logger) for a richer logging experience.

In the Scala API, markers are added through a MarkerContext trait, which is added as an implicit parameter to the logger methods, i.e.

```scala
import play.api._
logger.info("some info message")(MarkerContext(someMarker))
```

This opens the door for implicit markers to be passed for logging in several statements, which makes adding context to logging much easier without resorting to MDC.  For example, using [Logstash Logback Encoder](https://github.com/logstash/logstash-logback-encoder#loggingevent_custom_event) and an [implicit conversion chain](https://docs.scala-lang.org/tutorials/FAQ/chaining-implicits.html), request information can be encoded into logging statements automatically:

@[logging-request-context-trait](../../working/scalaGuide/main/logging/code/ScalaLoggingSpec.scala)

And then used in a controller and carried through `Future` that may use different execution contexts:

@[logging-log-info-with-request-context](../../working/scalaGuide/main/logging/code/ScalaLoggingSpec.scala)

Note that marker contexts are also very useful for "tracer bullet" style logging, where you want to log on a specific request without explicitly changing log levels.  For example, you can add a marker only when certain conditions are met:

@[logging-log-trace-with-tracer-controller](../../working/scalaGuide/main/logging/code/ScalaLoggingSpec.scala)

And then trigger logging with the following TurboFilter in `logback.xml`:

```xml
<turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
  <Name>TRACER_FILTER</Name>
  <Marker>TRACER</Marker>
  <OnMatch>ACCEPT</OnMatch>
</turboFilter>
```

For more information, please see [[ScalaLogging|ScalaLogging#Using-Markers-and-Marker-Contexts]] or [[JavaLogging|JavaLogging#Using-Markers]].

For more information about using Markers in logging, see [TurboFilters](https://logback.qos.ch/manual/filters.html#TurboFilter) and [marker based triggering](https://logback.qos.ch/manual/appenders.html#OnMarkerEvaluator) sections in the Logback manual.

## Configuration improvements

In the Java API, we have moved to the standard `Config` object from Lightbend's Config library instead of `play.Configuration`. This brings the behavior in line with standard config behavior, as the methods now expect all keys to exist. See [[the Java config migration guide|JavaConfigMigration26]] for migration details.

In the Scala API, we have introduced new methods to the `play.api.Configuration` class to simplify the API and allow loading of custom types. You can now use an implicit `ConfigLoader` to load any custom type you want. Like the `Config` API, the new `Configuration#get[T]` expects the key to exist by default and returns a value of type `T`, but there is also a `ConfigLoader[Option[T]]` that allows `null` config values. See the [[Scala configuration docs|ScalaConfig]] for more details.

## Security Logging

A security marker has been added for security related operations in Play, and failed security checks now log  at WARN level, with the security marker set.  This ensures that developers always know why a particular request is failing, which is important now that security filters are enabled by default in Play.

The security marker also allows security failures to be triggered or filtered distinct from normal logging.  For example, to disable all logging with the SECURITY marker set, add the following lines to the `logback.xml` file:

```xml
<turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
    <Marker>SECURITY</Marker>
    <OnMatch>DENY</OnMatch>
</turboFilter>
```

In addition, log events using the security marker can also trigger a message to a Security Information & Event Management (SIEM) engine for further processing.

## Configuring a Custom Logging Framework in Java

Before, if you want to [[use a custom logging framework|SettingsLogger#Using-a-Custom-Logging-Framework]], you had to configure it using Scala, even if the you have a Java project. Now it is possible to create custom `LoggerConfigurator` in both Java and Scala. To create a `LoggerConfigurator` in Java, you need to implement the given interface, for example, to configure Log4J:

```java
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.ILoggerFactory;
import play.Environment;
import play.LoggerConfigurator;
import play.Mode;
import play.api.PlayException;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.config.Configurator;

public class JavaLog4JLoggerConfigurator implements LoggerConfigurator {

    private ILoggerFactory factory;

    @Override
    public void init(File rootPath, Mode mode) {
        Map<String, String> properties = new HashMap<>();
        properties.put("application.home", rootPath.getAbsolutePath());

        String resourceName = "log4j2.xml";
        URL resourceUrl = this.getClass().getClassLoader().getResource(resourceName);
        configure(properties, Optional.ofNullable(resourceUrl));
    }

    @Override
    public void configure(Environment env) {
        Map<String, String> properties = LoggerConfigurator.generateProperties(env, ConfigFactory.empty(), Collections.emptyMap());
        URL resourceUrl = env.resource("log4j2.xml");
        configure(properties, Optional.ofNullable(resourceUrl));
    }

    @Override
    public void configure(Environment env, Config configuration, Map<String, String> optionalProperties) {
        // LoggerConfigurator.generateProperties enables play.logger.includeConfigProperties=true
        Map<String, String> properties = LoggerConfigurator.generateProperties(env, configuration, optionalProperties);
        URL resourceUrl = env.resource("log4j2.xml");
        configure(properties, Optional.ofNullable(resourceUrl));
    }

    @Override
    public void configure(Map<String, String> properties, Optional<URL> config) {
        try {
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            loggerContext.setConfigLocation(config.get().toURI());

            factory = org.slf4j.impl.StaticLoggerBinder.getSingleton().getLoggerFactory();
        } catch (URISyntaxException ex) {
            throw new PlayException(
                "log4j2.xml resource was not found",
                "Could not parse the location for log4j2.xml resource",
                ex
            );
        }
    }

    @Override
    public ILoggerFactory loggerFactory() {
        return factory;
    }

    @Override
    public void shutdown() {
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext();
        Configurator.shutdown(loggerContext);
    }
}
```

> **Note**: this implementation is fully compatible with Scala version `LoggerConfigurator` and can even be used in Scala projects if necessary, which means that module creators can provide a Java or Scala implementation of LoggerConfigurator and they will be usable in both Java and Scala projects.

## Separate Java Forms module and PlayMinimalJava plugin

The [[Java forms|JavaForms]] functionality has been split out into a separate module. The forms functionality depends on a few Spring modules and the Hibernate validator, so if you are not using forms, you may wish to remove the Java forms module to avoid those unnecessary dependencies.

This module is automatically included by the `PlayJava` plugin, but can be disabled by using the `PlayMinimalJava` plugin instead:

```
lazy val root = (project in file("."))
  .enablePlugins(PlayMinimalJava)
```

## Java Compile Time Components

Just as in Scala, Play now has components to enable [[Java Compile Time Dependency Injection|JavaCompileTimeDependencyInjection]]. The components were created as interfaces that you should `implements` and they provide default implementations. There are components for all the types that could be injected when using [[Runtime Dependency Injection|JavaDependencyInjection]]. To create an application using Compile Time Dependency Injection, you just need to provide an implementation of `play.ApplicationLoader` that uses a custom implementation of `play.BuiltInComponents`, for example:

```java
import play.routing.Router;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.HttpFiltersComponents;

public class MyComponents extends BuiltInComponentsFromContext
        implements HttpFiltersComponents {

    public MyComponents(ApplicationLoader.Context context) {
        super(context);
    }

    @Override
    public Router router() {
        return Router.empty();
    }
}
```

The `play.ApplicationLoader`:

```java
import play.ApplicationLoader;

public class MyApplicationLoader implements ApplicationLoader {

    @Override
    public Application load(Context context) {
        return new MyComponents(context).application();
    }

}
```

And configure `MyApplicationLoader` as explained in [[Java Compile-Time Dependency Injection docs|JavaCompileTimeDependencyInjection#Application-entry-point]].

## Improved Form Handling I18N support

The `MessagesApi` and `Lang` classes are used for internationalization in Play, and are required to display error messages in forms.

In the past, putting together a form in Play has required [multiple steps](https://www.theguardian.com/info/developer-blog/2015/dec/30/how-to-add-a-form-to-a-play-application), and the creation of a `Messages` instance from a request was not discussed in the context of form handling.

In addition, it was inconvenient to have a `Messages` instance passed through all template fragments when form handling was required, and `Messages` implicit support was provided directly through the controller trait.  The I18N API has been refined with the addition of a `MessagesProvider` trait, implicits that are tied directly to requests, and the forms documentation has been improved.

The [`MessagesActionBuilder`](api/scala/play/api/mvc/MessagesActionBuilder.html) has been added.  This action builder provides a [`MessagesRequest`](api/scala/play/api/mvc/MessagesRequest.html), which is a [`WrappedRequest`](api/scala/play/api/mvc/WrappedRequest.html) that extends [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html), only a single implicit parameter needs to be made available to templates, and you don't need to extend `Controller` with `I18nSupport`.  This is also useful because to use [[CSRF|ScalaCsrf]] with forms, both a `Request` (technically a `RequestHeader`) and a `Messages` object must be available to the template.

```scala
class FormController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
  extends AbstractController(components) {

  import play.api.data.Form
  import play.api.data.Forms._

  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(UserData.apply)(UserData.unapply)
  )

  def index = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.displayForm(userForm))
  }

  def post = ...
}
```

where `displayForm.scala.html` is defined as:

```twirl
@(userForm: Form[UserData])(implicit request: MessagesRequestHeader)

@import helper._

@helper.form(action = routes.FormController.post()) {
  @CSRF.formField                     @* <- takes a RequestHeader    *@
  @helper.inputText(userForm("name")) @* <- takes a MessagesProvider *@
  @helper.inputText(userForm("age"))  @* <- takes a MessagesProvider *@
}
```

For more information, please see [[ScalaI18N]].

### Testing Support

Support for creating `MessagesApi` instances has been improved.  Now, when you want to create a [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) instance, you can create [`DefaultMessagesApi()`](api/scala/play/api/i18n/DefaultMessagesApi.html) or [`DefaultLangs()`](api/scala/play/api/i18n/DefaultLangs.html) with default arguments.  If you want to specify test messages from configuration or from another source, you can pass in those values:

```scala
val messagesApi: MessagesApi = {
    val env = new Environment(new File("."), this.getClass.getClassLoader, Mode.Dev)
    val config = Configuration.reference ++ Configuration.from(Map("play.i18n.langs" -> Seq("en", "fr", "fr-CH")))
    val langs = new DefaultLangsProvider(config).get
    new DefaultMessagesApi(testMessages, langs)
  }
```

## Future Timeout and Delayed Support

Play's support for futures in asynchronous operations has been improved, using the `Futures` trait.

You can use the [`play.libs.concurrent.Futures`](api/java/play/libs/concurrent/Futures.html) interface to wrap a `CompletionStage` in a non-blocking timeout:

```java
class MyClass {
    @Inject
    public MyClass(Futures futures) {
        this.futures = futures;
    }

    CompletionStage<Double> callWithOneSecondTimeout() {
        return futures.timeout(computePIAsynchronously(), Duration.ofSeconds(1));
    }
}
```

or use [`play.api.libs.concurrent.Futures`](api/scala/play/api/libs/concurrent/Futures.html) trait in the Scala API:

```scala
import play.api.libs.concurrent.Futures._

class MyController @Inject()(cc: ControllerComponents)(implicit futures: Futures) extends AbstractController(cc) {

  def index = Action.async {
    // withTimeout is an implicit type enrichment provided by importing Futures._
    intensiveComputation().withTimeout(1.seconds).map { i =>
      Ok("Got result: " + i)
    }.recover {
      case e: TimeoutException =>
        InternalServerError("timeout")
    }
  }
}
```

There is also a `delayed` method which only executes a `Future` after a specified delay, which works similarly to timeout.

For more information, please see [[ScalaAsync]] or [[JavaAsync]].

## CustomExecutionContext and Thread Pool Sizing

This class defines a custom execution context that delegates to an `akka.actor.ActorSystem`.  It is very useful for situations in which the default execution context should not be used, for example if a database or blocking I/O is being used.  Detailed information can be found in the [[ThreadPools]] page, but Play 2.6.x adds a `CustomExecutionContext` class that handles the underlying Akka dispatcher lookup.

## Updated Templates with Preconfigured CustomExecutionContexts

All of the Play example templates on [Play's download page](https://playframework.com/download#examples) that use blocking APIs (i.e. Anorm, JPA) have been updated to use custom execution contexts where appropriate.  For example, going to https://github.com/playframework/play-java-jpa-example/ shows that the [JPAPersonRepository](https://github.com/playframework/play-java-jpa-example/blob/4f962bc/app/models/JPAPersonRepository.java) class takes a `DatabaseExecutionContext` that wraps all the database operations.

For thread pool sizing involving JDBC connection pools, you want a fixed thread pool size matching the connection pool, using a thread pool executor.  Following the advice in [HikariCP's pool sizing page](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing), you should configure your JDBC connection pool to double the number of physical cores, plus the number of disk spindles.

The dispatcher settings used here come from [Akka dispatcher](https://doc.akka.io/docs/akka/2.5/dispatchers.html?language=java):

```
# db connections = ((physical_core_count * 2) + effective_spindle_count)
fixedConnectionPool = 9

database.dispatcher {
  executor = "thread-pool-executor"
  throughput = 1
  thread-pool-executor {
    fixed-pool-size = ${fixedConnectionPool}
  }
}
```

### Defining a CustomExecutionContext in Scala

To define a custom execution context, subclass [`CustomExecutionContext`](api/scala/play/api/libs/concurrent/CustomExecutionContext.html) with the dispatcher name:

```scala
@Singleton
class DatabaseExecutionContext @Inject()(system: ActorSystem)
   extends CustomExecutionContext(system, "database.dispatcher")
```

Then have the execution context passed in as an implicit parameter:

```scala
class DatabaseService @Inject()(implicit executionContext: DatabaseExecutionContext) {
  ...
}
```

### Defining a CustomExecutionContext in Java

To define a custom execution context, subclass [`CustomExecutionContext`](api/java/play/libs/concurrent/CustomExecutionContext.html) with the dispatcher name:

```java
import akka.actor.ActorSystem;
import play.libs.concurrent.CustomExecutionContext;

public class DatabaseExecutionContext
        extends CustomExecutionContext {

    @javax.inject.Inject
    public DatabaseExecutionContext(ActorSystem actorSystem) {
        // uses a custom thread pool defined in application.conf
        super(actorSystem, "database.dispatcher");
    }
}
```

Then pass the JPA context in explicitly:

```java
public class JPAPersonRepository implements PersonRepository {

    private final JPAApi jpaApi;
    private final DatabaseExecutionContext executionContext;

    @Inject
    public JPAPersonRepository(JPAApi jpaApi, DatabaseExecutionContext executionContext) {
        this.jpaApi = jpaApi;
        this.executionContext = executionContext;
    }

    ...
}
```

## Play `WSClient` Improvements

There are substantial improvements to Play `WSClient`.  Play `WSClient` is now a wrapper around the standalone [play-ws](https://github.com/playframework/play-ws) implementation, which can be used outside of Play.  In addition, the underlying libraries involved in [play-ws](https://github.com/playframework/play-ws) have been [shaded](https://github.com/sbt/sbt-assembly#shading), so that the Netty implementation used in it does not conflict with Spark, Play or any other library that uses a different version of Netty.

Finally, there is now support for [HTTP Caching](https://tools.ietf.org/html/rfc7234) if a cache implementation is present.  Using an HTTP cache means savings on repeated requests to backend REST services, and is especially useful when combined with resiliency features such as [`stale-on-error` and `stale-while-revalidate`](https://tools.ietf.org/html/rfc5861).

For more details, please see [[WsCache]] and the [[WS Migration Guide|WSMigration26]].

## Play JSON improvements

There are many improvements included in this release of the JSON library.

### Ability to serialize tuples

Now, tuples are able to be serialized by play-json, and there are `Reads` and `Writes` implementations in the implicit scope. Tuples are serialized to arrays, so `("foo", 2, "bar")` will render as `["foo", 2, "bar"]` in the JSON.

### Scala.js support

Play JSON 2.6.0 now supports Scala.js. You can add the dependency with:

```scala
libraryDependencies += "com.typesafe.play" %%% "play-json" % version
```

where `version` is the version you wish to use. The library should effectively work the same as it does on the JVM, except without support for JVM types.

### Custom naming strategies for automated JSON mapping

It is possible to customize the handlers generated by the `Json` macros (`reads`, `writes` or `format`). Thus, a naming strategy can be defined to map the JSON fields as wanted.

To use a custom naming strategy you need to define implicit instances for `JsonConfiguration` and `JsonNaming`.

Two naming strategies are provided: the default one, using as-is the names of the class properties, and the `JsonNaming.SnakeCase` case one.

A strategy other than the default one can be used as following:

```scala
import play.api.libs.json._

implicit val config = JsonConfiguration(SnakeCase)

implicit val userFormat: OFormat[PlayUser] = Json.format[PlayUser]
```

In addition, custom naming strategies can be implemented by providing a `JsonNaming` implementation.

## Testing Improvements

Some utility classes have been added to the `play.api.test` package in 2.6.x to make functional testing easier with dependency injected components.

### Injecting

There are many functional tests that use the injector directly through the implicit `app`:

```scala
"test" in new WithApplication() {
  val executionContext = app.injector.instanceOf[ExecutionContext]
  ...
}
```

Now with the [`Injecting`](api/scala/play/api/test/Injecting.html) trait, you can elide this:

```scala
"test" in new WithApplication() with Injecting {
  val executionContext = inject[ExecutionContext]
  ...
}
```

### StubControllerComponents

The [`StubControllerComponentsFactory`](api/scala/play/api/test/StubControllerComponentsFactory.html) creates a stub [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) that can be used for unit testing a controller:

```scala
val controller = new MyController(stubControllerComponents())
```

### StubBodyParser

The [`StubBodyParserFactory`](api/scala/play/api/test/StubBodyParserFactory.html) creates a stub [`BodyParser`](api/scala/play/api/mvc/BodyParser.html) that can be used for unit testing content:

```scala
val stubParser = stubBodyParser(AnyContent("hello"))
```

## File Upload Improvements

Uploading files uses a `TemporaryFile` API which relies on storing files in a temporary filesystem, as specified in [[ScalaFileUpload]] / [[JavaFileUpload]], accessible through the `ref` attribute.

Uploading files is an inherently dangerous operation, because unbounded file upload can cause the filesystem to fill up -- as such, the idea behind `TemporaryFile` is that it's only in scope at completion and should be moved out of the temporary file system as soon as possible.  Any temporary files that are not moved are deleted.

In 2.5.x, TemporaryFile were deleted as the file references were garbage collected, using `finalize`.   However, under [certain conditions](https://github.com/playframework/playframework/issues/5545), garbage collection did not occur in a timely fashion.  The background cleanup has been moved to use [FinalizableReferenceQueue](https://google.github.io/guava/releases/20.0/api/docs/com/google/common/base/FinalizableReferenceQueue.html) and PhantomReferences rather than use `finalize`.

The Java and Scala APIs for `TemporaryFile` has been reworked so that all `TemporaryFile` references come from a `TemporaryFileCreator` trait, and the implementation can be swapped out as necessary, and there's now an [`atomicMoveWithFallback`](api/scala/play/api/libs/Files$$TemporaryFile.html#atomicMoveWithFallback\(to:java.nio.file.Path\):play.api.libs.Files.TemporaryFile) method that uses `StandardCopyOption.ATOMIC_MOVE` if available.

### TemporaryFileReaper

There's also now a [`play.api.libs.Files.TemporaryFileReaper`](api/scala/play/api/libs/Files$$DefaultTemporaryFileReaper.html) that can be enabled to delete temporary files on a scheduled basis using the Akka scheduler, distinct from the garbage collection method.

The reaper is disabled by default, and is enabled through `application.conf`:

```
play.temporaryFile {
  reaper {
    enabled = true
    initialDelay = "5 minutes"
    interval = "30 seconds"
    olderThan = "30 minutes"
  }
}
```

The above configuration will delete files that are more than 30 minutes old, using the "olderThan" property.  It will start the reaper five minutes after the application starts, and will check the filesystem every 30 seconds thereafter.  The reaper is not aware of any existing file uploads, so protracted file uploads may run into the reaper if the system is not carefully configured.
