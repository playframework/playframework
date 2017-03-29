# What's new in Play 2.6

This page highlights the new features of Play 2.6. If you want to learn about the changes you need to make when you migrate to Play 2.6, check out the [[Play 2.6 Migration Guide|Migration26]].

## Akka HTTP Server Backend

Play now uses the [Akka-HTTP](http://doc.akka.io/docs/akka-http/current/scala.html) server engine as the default backend.  More detail about Play's integration with Akka-HTTP can be found [[on the Akka HTTP Server page|AkkaHttpServer]].  There is an additional page on [[configuring Akka HTTP|SettingsAkkaHttp]].

The Netty backend is still available, and has been upgraded to use Netty 4.1.  You can explicitly configure your project to use Netty [[on the NettyServer page|NettyServer]].

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
Request newReq = req.withAttrs(req.attrs().put(Attrs.USER, newUser));
```

Scala:
```scala
// Create a TypedKey to store a User object
object Attrs {
  val User: TypedKey[User] = TypedKey[User].apply("user")
}

// Get the User object from the request
val user: User = req.attrs(Attrs.User)
// Put a User object into the request
val newReq = req.withAttrs(req.attrs.updated(Attrs.User, newUser))
```

Attributes are stored in a `TypedMap`. You can read more about attributes in the `TypedMap` documentation: [Javadoc](api/java/play/libs/typedmap/TypedMap.html), [Scaladoc](api/scala/play/api/libs/typedmap/TypedMap.html).

Request tags have now been deprecated and you should migrate to use attributes instead. See the [[tags section|Migration26#Request-tags-deprecation]] in the migration docs for more information.

## Injectable Twirl Templates

Twirl templates can now be created with a constructor annotation using `@this`.  The constructor annotation means that Twirl templates can be injected into templates directly and can manage their own dependencies, rather than the controller having to manage dependencies not only for itself, but also for the templates it has to render.

As an example, suppose a template has a dependency on a component `TemplateRenderingComponent`, which is not used by the controller.

First, add the `@Inject` annotation to Twirl in `build.sbt`:

```scala
TwirlKeys.constructorAnnotations += "@javax.inject.Inject()"
```

Then create a file `IndexTemplate.scala.html` using the `@this` syntax for the constructor. Note that the constructor must be placed **before** the `@()` syntax used for the template's parameters for the `apply` method:

```scala
@this(trc: TemplateRenderingComponent)
@()

@{trc.render(item)}
```

And finally define the controller by injecting the template in the constructor:

```scala
public MyController @Inject()(indexTemplate: views.html.IndexTemplate,
                              cc: ControllerComponents)
  extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(indexTemplate())
  }
}
```

or

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

Once the template is defined with its dependencies, then the controller can have the template injected into the controller, but the controller does not see `TemplateRenderingComponent`.

## Filters Enhancements

Play now comes with a default set of enabled filters, defined through configuration.

* `play.filters.csrf.CSRFFilter`
* `play.filters.headers.SecurityHeadersFilter`
* `play.filters.hosts.AllowedHostsFilter`

This means that on new projects, CSRF protection ([[ScalaCsrf]] / [[JavaCsrf]]), [[SecurityHeaders]] and [[AllowedHostsFilter]] are all defined automatically.  This provides a "secure by default" experience for new Play applications, and tightens security on existing Play applications.

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

This opens the door for implicit markers to be passed for logging in several statements, which makes adding context to logging much easier without resorting to MDC.  In particular, see what you can do with the [Logstash Logback Encoder](https://github.com/logstash/logstash-logback-encoder#event-specific-custom-fields):

```scala
implicit def requestToMarkerContext[A](request: Request[A]): MarkerContext = {
  import net.logstash.logback.marker.LogstashMarker
  import net.logstash.logback.marker.Markers._

  val requestMarkers: LogstashMarker = append("host", request.host)
    .and(append("path", request.path))

  MarkerContext(requestMarkers)
}

def index = Action { request =>
  logger.debug("index: ")(request)
  Ok("testing")
}
```

Note that markers are also very useful for "tracer bullet" style logging, where you want to log on a specific request without explicitly changing log levels:

```scala
package controllers

import javax.inject._
import play.api.mvc._

@Singleton
class TracerBulletController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  private val logger = org.slf4j.LoggerFactory.getLogger("application")

  // in logback.xml
  /*
  <turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
    <Name>TRACER_FILTER</Name>
    <Marker>TRACER</Marker>
    <OnMatch>ACCEPT</OnMatch>
  </turboFilter>
   */
  private val tracerMarker = org.slf4j.MarkerFactory.getMarker("TRACER")

  private def generateMarker(implicit request: RequestHeader): org.slf4j.Marker = {
    val marker = org.slf4j.MarkerFactory.getDetachedMarker("dynamic") // base do-nothing marker...
    if (request.getQueryString("trace").nonEmpty) {
        marker.add(tracerMarker)
    }
    marker
  }

  def index = Action { implicit request =>
    val marker = generateMarker
    if (logger.isTraceEnabled(marker)) {
      logger.trace(marker, "Hello world!")
    }
    Ok("hello world")
  }
}
```

For more information, please see [[ScalaLogging]] or [[JavaLogging]].

For more information about using Markers in logging, see [TurboFilters](https://logback.qos.ch/manual/filters.html#TurboFilter) and [marker based triggering](https://logback.qos.ch/manual/appenders.html#OnMarkerEvaluator) sections in the Logback manual.

## Improved Form Handling I18N support

The `MessagesApi` and `Lang` classes are used for internationalization in Play, and are required to display error messages in forms. 
 
 In the past, putting together a form in Play has required [multiple steps](https://www.theguardian.com/info/developer-blog/2015/dec/30/how-to-add-a-form-to-a-play-application), and the creation of a `Messages` instance from a request was not discussed in the context of form handling. 
  
In addition, it was inconvenient to have a `Messages` instance passed through all template fragments when form handling was required, and `Messages` implicit support was provided directly through the controller trait.  The I18N API has been refined with the addition of a `MessagesProvider` trait, implicits that are tied directly to requests, and the forms documentation has been improved.

For more information, please see [[ScalaI18N]] or [[JavaI18N]].



