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

This means that on new projects, CSRF protection ([[ScalaCsrf]] / [[JavaCsrf]]), [[SecurityHeaders]] and [[AllowedHostsFilter]] are all defined automatically.

In addition, filters can now be configured through `application.conf`.  To append to the defaults list, use the `+=`:

```
play.filters.enabled+=MyFilter
```

If you want to specifically disable a filter for testing, you can also do that from configuration:

```
play.filters.disabled+=MyFilter
```

Please see [[the Filters page|Filters]] for more details.