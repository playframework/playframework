<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Play 2.6 Migration Guide

This is a guide for migrating from Play 2.5 to Play 2.6. If you need to migrate from an earlier version of Play then you must first follow the [[Play 2.5 Migration Guide|Migration25]].

## How to migrate

The following steps need to be taken to update your sbt build before you can load/run a Play project in sbt.

### Play upgrade

Update the Play version number in `project/plugins.sbt` to upgrade Play:

```scala
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.x")
```

Where the "x" in `2.6.x` is the minor version of Play you want to use, for instance `2.6.0`.

### sbt upgrade to 0.13.15

Play 2.6 requires upgrading to at least sbt 0.13.15. The 0.13.15 release of sbt has a number of [improvements and bug fixes](https://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#sbt+0.13.15) (see also the changes in [sbt 0.13.13](https://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#sbt+0.13.13)). 

sbt 1.x is supported beginning with Play 2.6.6. If you are using other sbt plugins, you may need to check if there is a newer version compatible with sbt 1.x

To update, change your `project/build.properties` so that it reads:

```
sbt.version=0.13.15
```

### Guice DI support moved to separate module

In Play 2.6, the core Play module no longer includes Guice. You will need to configure the Guice module by adding `guice` to your `libraryDependencies`:

```scala
libraryDependencies += guice
```

### OpenID support moved to separate module

In Play 2.6, the core Play module no longer includes the OpenID support in `play.api.libs.openid` (Scala) and `play.libs.openid` (Java). To use these packages add `openId` to your `libraryDependencies`:

```scala
libraryDependencies += openId
```

### Play JSON moved to separate project

Play JSON has been moved to a separate library hosted at https://github.com/playframework/play-json. Since Play JSON has no dependencies on the rest of Play, the main change is that the `json` value from `PlayImport` will no longer work in your sbt build. Instead, you'll have to specify the library manually:

```scala
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
```

Also, play-json has a separate release cycle from the core Play library, so the version is no longer in sync with the Play version.

### Play Iteratees moved to separate project

Play Iteratees has been moved to a separate library hosted at https://github.com/playframework/play-iteratees. Since Play Iteratees has no dependencies on the rest of Play, the main change is that the you'll have to specify the library manually:

```scala
libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1"
```

The project also has a sub project that integrates Iteratees with [Reactive Streams](http://www.reactive-streams.org/). You may need to add the following dependency as well:

```scala
libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"
```

> **Note**: The helper class `play.api.libs.streams.Streams` was moved to `play-iteratees-reactive-streams` and now is called `play.api.libs.iteratee.streams.IterateeStreams`. So you may need to add the Iteratees dependencies and also use the new class where necessary.

Finally, Play Iteratees has a separate versioning scheme, so the version is no longer in sync with the Play version.

## Akka HTTP as the default server engine

Play now uses the [Akka-HTTP](https://doc.akka.io/docs/akka-http/current/?language=scala) server engine as the default backend. If you need to change it back to Netty for some reason (for example, if you are using Netty's [native transports](https://netty.io/wiki/native-transports.html)), see how to do that in [[Netty Server|NettyServer]] documentation.

You can read more at [[Akka HTTP Server Backend|AkkaHttpServer]].

### Akka HTTP server timeouts

Play 2.5.x does not have a request timeout configuration for [[Netty Server|NettyServer]], which was the default server backend. But Akka HTTP has timeouts for both idle connections and requests (see more details in [[Akka HTTP Settings|SettingsAkkaHttp]] documentation). [Akka HTTP docs](https://doc.akka.io/docs/akka-http/current/common/timeouts.html?language=scala#akka-http-timeouts) states that:

> Akka HTTP comes with a variety of built-in timeout mechanisms to protect your servers from malicious attacks or programming mistakes.

And you can see the default values for `akka.http.server.idle-timeout`, `akka.http.server.request-timeout` and `akka.http.server.bind-timeout` [here](https://doc.akka.io/docs/akka-http/current/configuration.html?language=scala). Play has [[its own configurations to define timeouts|SettingsAkkaHttp]], so if you start to see a number of `503 Service Unavailable`, you can change the configurations to values that are more reasonable to your application, for example:

```
play.server.http.idleTimeout = 60s
play.server.akka.requestTimeout = 40s
```

## Scala `Mode` changes

Scala [`Mode`](api/scala/play/api/Mode.html) was refactored from an Enumeration to a hierarchy of case objects. Most of the Scala code won't change because of this refactoring. But, if you are accessing the Scala `Mode` values in your Java code, you will need to change it from:

```java
// Consider this Java code
play.api.Mode scalaMode = play.api.Mode.Test();
```

Must be rewritten to:

```java
// Consider this Java code
play.api.Mode scalaMode = play.Mode.TEST.asScala();
```

It is also easier to convert between Java and Scala modes:

```java
// In your Java code
play.api.Mode scalaMode = play.Mode.DEV.asScala();
```

Or in your Scala code:

```scala
play.Mode javaMode = play.api.Mode.Dev.asJava
```

Also, `play.api.Mode.Mode` is now deprecated and you should use `play.api.Mode` instead.

## `Writeable[JsValue]` changes

Previously, the default Scala `Writeable[JsValue]` allowed you to define an implicit `Codec`, which would allow you to write using a different charset. This could be a problem since `application/json` does not act like text-based content types. It only allows Unicode charsets (`UTF-8`, `UTF-16` and `UTF-32`) and does not define a `charset` parameter like many text-based content types.

Now, the default `Writeable[JsValue]` takes no implicit parameters and always writes to `UTF-8`. This covers the majority of cases, since most users want to use UTF-8 for JSON. It also allows us to easily use more efficient built-in methods for writing JSON to a byte array.

If you need the old behavior back, you can define a `Writeable` with an arbitrary codec using `play.api.http.Writeable.writeableOf_JsValue(codec, contentType)` for your desired Codec and Content-Type.

## Scala Controller changes

The idiomatic Play controller has in the past required global state. The main places that was needed was in the global [`Action`](api/scala/play/api/mvc/Action$.html) object and [`BodyParsers#parse`](api/scala/play/api/mvc/BodyParsers.html#parse:play.api.mvc.PlayBodyParsers) method.

We have provided several new controller classes with new ways of injecting that state, providing the same syntax:
 - [`BaseController`](api/scala/play/api/mvc/BaseController.html): a trait with an abstract [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) that can be provided by an implementing class.
 - [`AbstractController`](api/scala/play/api/mvc/AbstractController.html): an abstract class extending [`BaseController`](api/scala/play/api/mvc/BaseController.html) with a [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) constructor parameter that can be injected using constructor injection.
 - [`InjectedController`](api/scala/play/api/mvc/InjectedController.html): a trait, extending [`BaseController`](api/scala/play/api/mvc/BaseController.html), that obtains the [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) through method injection (calling a `setControllerComponents` method). If you are using a runtime DI framework like Guice, this is done automatically.

[`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) is simply meant to bundle together components typically used in a controller. You may also wish to create your own base controller for your app by extending [`ControllerHelpers`](api/scala/play/api/mvc/ControllerHelpers.html) and injecting your own bundle of components. Play does not require your controllers to implement any particular trait.

Note that [`BaseController`](api/scala/play/api/mvc/BaseController.html) makes [`Action`](api/scala/play/api/mvc/Action.html) and `parse` refer to injected instances rather than the global objects, which is usually what you want to do.

Here's an example of code using [`AbstractController`](api/scala/play/api/mvc/AbstractController.html):

```scala
class FooController @Inject() (components: ControllerComponents)
    extends AbstractController(components) {

  // Action and parse now use the injected components
  def foo = Action(parse.json) {
    Ok
  }
}
```

and using [`BaseController`](api/scala/play/api/mvc/BaseController.html):

```scala
class FooController @Inject() (val controllerComponents: ControllerComponents) extends BaseController {

  // Action and parse now use the injected components
  def foo = Action(parse.json) {
    Ok
  }
}
```

and [`InjectedController`](api/scala/play/api/mvc/InjectedController.html):

```scala
class FooController @Inject() () extends InjectedController {

  // Action and parse now use the injected components
  def foo = Action(parse.json) {
    Ok
  }
}
```

[`InjectedController`](api/scala/play/api/mvc/InjectedController.html) gets its [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) by calling the `setControllerComponents` method, which is called automatically by JSR-330 compliant dependency injection. We do not recommend using [`InjectedController`](api/scala/play/api/mvc/InjectedController.html) with compile-time injection. If you plan to extensively unit test your controllers manually, we also recommend avoiding [`InjectedController`](api/scala/play/api/mvc/InjectedController.html) since it hides the dependency.

If you prefer to pass the individual dependencies manually, you can do that instead and extend [`ControllerHelpers`](api/scala/play/api/mvc/ControllerHelpers.html), which has no dependencies or state. Here's an example:

```scala
class Controller @Inject() (
    action: DefaultActionBuilder,
    parse: PlayBodyParsers,
    messagesApi: MessagesApi
  ) extends ControllerHelpers {
  def index = action(parse.text) { request =>
    Ok(messagesApi.preferred(request)("hello.world"))
  }
}
```

## Scala ActionBuilder and BodyParser changes

The Scala [`ActionBuilder`](api/scala/play/api/mvc/ActionBuilder.html) trait has been modified to specify the type of the body as a type parameter, and add an abstract `parser` member as the default body parsers. You will need to modify your ActionBuilders and pass the body parser directly.

The [`Action`](api/scala/play/api/mvc/Action$.html) global object and [`BodyParsers#parse`](api/scala/play/api/mvc/BodyParsers.html#parse:play.api.mvc.PlayBodyParsers) are now deprecated. They are replaced by injectable traits, [`DefaultActionBuilder`](api/scala/play/api/mvc/DefaultActionBuilder.html) and [`PlayBodyParsers`](api/scala/play/api/mvc/PlayBodyParsers.html) respectively. If you are inside a controller, they are automatically provided by the new [`BaseController`](api/scala/play/api/mvc/BaseController.html) trait (see [the controller changes](#Scala-Controller-changes) above).

## Cookies

For Java users, we now recommend using [`play.mvc.Http.Cookie.builder`](api/java/play/mvc/Http.Cookie.html#builder-java.lang.String-java.lang.String-) to create new cookies, for example:

```java
Http.Cookie cookie = Cookie.builder("color", "blue")
  .withMaxAge(3600)
  .withSecure(true)
  .withHttpOnly(true)
  .withSameSite(SameSite.STRICT)
  .build();
```

This is more readable than a plain constructor call, and will be source-compatible if we add/remove cookie attributes in the future.

### SameSite attribute, enabled for session and flash

Cookies now can have an additional [`SameSite` attribute](https://tools.ietf.org/html/draft-ietf-httpbis-cookie-same-site-00), which can be used to prevent CSRF. There are three possible states:

 - No `SameSite`, meaning cookies will be sent for all requests to that domain.
 - `SameSite=Strict`, meaning the cookie will only be sent for same-site requests (coming from another page on the site) not cross-site requests
 - `SameSite=Lax`, meaning the cookie will be sent for cross-site requests as top-level navigation, but otherwise only for same-site requests. This will do the correct thing for most sites, but won't prevent certain types of attacks, such as those executed by launching popup windows.

In addition, we have moved the session and flash cookies to use `SameSite=Lax` by default. You can tweak this using configuration. For example:

```
play.http.session.sameSite = null // no same-site for session
play.http.flash.sameSite = "strict" // strict same-site for flash
```

> **Note**: this feature is currently [not supported by many browsers](https://caniuse.com/#feat=same-site-cookie-attribute), so you should not rely on it. Chrome and Opera are the only major browsers to support SameSite right now.

### __Host and __Secure prefixes

We've also added support for the [\__Host and \__Secure cookie name prefixes](https://tools.ietf.org/html/draft-ietf-httpbis-cookie-prefixes-00#section-3).

This will only affect you if you happen to be using these prefixes for cookie names. If you are, Play will warn when serializing and deserializing those cookies if the proper attributes are not set, then set them for you automatically. To remove the warning, either cease using those prefixes for your cookies, or be sure to set the attributes as follows:

- Cookies named with `__Host-` should set `Path=/` and `Secure` attributes.
- Cookies named with `__Secure-` should set the `Secure` attribute.

## Assets

### Binding Assets with compile-time DI

If you are using compile-time DI, you should mix in [`controllers.AssetsComponents`](api/scala/controllers/AssetsComponents.html) and use that to obtain the `assets: Assets` controller instance:

```scala
class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) with AssetsComponents {
  lazy val router = new Routes(httpErrorHandler, assets)
}
```

If you have an existing `lazy val assets: Assets` you can remove it.

### Assets configuration

Existing user-facing APIs have not changed, but we suggest moving over to the [`AssetsFinder`](api/scala/controllers/AssetsFinder.html) API for finding assets and setting up your assets directories in configuration:

```
play.assets {
  path = "/public"
  urlPrefix = "/assets"
}
```

Then in routes you can do:

```
# prefix must match `play.assets.urlPrefix`
GET /assets/*file           controllers.Assets.at(file)
GET /versionedAssets/*file  controllers.Assets.versioned(file)
```

You no longer need to provide an assets path at the start of the argument list, since that's now read from configuration.

Then in your template you can use [`AssetsFinder#path`](api/scala/controllers/AssetsFinder.html#path\(rawPath:String\):String) to find the final path of the asset:

```scala
@(assets: AssetsFinder)

<img alt="hamburger" src="@assets.path("images/hamburger.jpg")">
```

You can still continue to use reverse routes with `Assets.versioned`, but some global state is required to convert the asset name you provide to the final asset name, which can be problematic if you want to run multiple applications at once.

## Form changes

Starting with Play 2.6, query string parameters will not be bound to a form instance anymore when using [`bindFromRequest()`](api/scala/play/api/data/Form.html#bindFromRequest\(\)\(implicitrequest:play.api.mvc.Request[_]\):play.api.data.Form[T]) in combination with `POST`, `PUT` or `PATCH` requests.

Static methods which were already deprecated in 2.5 (e.g. `DynamicForm.form()`) were removed in this release. Refer to the [[Play 2.5 Migration Guide|Migration25]] for details on how to migrate, in case you still use them.

### Java Form Changes

The [`errors()`](api/java/play/data/Form.html#errors--) method of a [`play.data.Form`](api/java/play/data/Form.html) instance is now deprecated. You should use `allErrors()` instead now which returns a simple `List<ValidationError>` instead of a `Map<String,List<ValidationError>>`. Where before Play 2.6 you called `.errors().get("key")` you can now simply call `.errors("key")`.

From now on, a `validate` method implemented inside a form class (usually used for cross field validation) is part of a class-level constraint. Check out the [[Advanced validation|JavaForms#advanced-validation]] docs for further information on how to use such constraints.
Existing `validate` methods can easily be migrated by annotating the affected form classes with `@Validate` and, depending on the return type of the validate method, by implementing the [`Validatable`](api/java/play/data/validation/Constraints.Validatable.html) interface with the applicable type argument (all defined in [`play.data.validation.Constraints`](api/java/play/data/validation/Constraints.html)):

| **Return type**                                                                    | **Interface to implement**
| -----------------------------------------------------------------------------------|-------------------------------------
| `String`                                                                           | `Validatable<String>`
| `ValidationError`                                                                  | `Validatable<ValidationError>`
| `List<ValidationError>`                                                            | `Validatable<List<ValidationError>>`
| `Map<String,List<ValidationError>>`<br>(not supported anymore; use `List` instead) | `Validatable<List<ValidationError>>`

For example an existing form like:

```java
public class MyForm {
    //...
    public String validate() {
        //...
    }
}
```

Has to be changed to:

```java
import play.data.validation.Constraints.Validate;
import play.data.validation.Constraints.Validatable;

@Validate
public class MyForm implements Validatable<String> {
    //...
    @Override
    public String validate() {
        //...
    }
}
```

> **Be aware**: The "old" `validate` method was invoked only after all other constraints were successful before. By default class-level constraints however are called simultaneously with any other constraint annotations - no matter if they passed or failed. To (also) define an order between the constraints you can now use [[constraint groups|JavaForms#defining-the-order-of-constraint-groups]].

## JPA Migration Notes

See [[JPA migration notes|JPAMigration26]].

## I18n Migration Notes

See [[I18N API Migration|MessagesMigration26]].

## Cache APIs Migration Notes

See [[Cache APIs Migration|CacheMigration26]].

## Java Configuration API Migration Notes

See [[Java Configuration Migration|JavaConfigMigration26]].

## Scala Configuration API

The Scala [`play.api.Configuration`](api/scala/play/api/Configuration.html) API now has new methods that allow loading any type using a [`ConfigLoader`](api/scala/play/api/ConfigLoader.html). These new methods expect configuration keys to exist in the configuration file. For example, the following old code:

```scala
val myConfig: String = configuration.getString("my.config.key").getOrElse("default")
```
should be changed to
```scala
val myConfig: String = configuration.get[String]("my.config.key")
```
and the value "default" should be set in configuration as `my.config.key = default`.

Alternatively, if custom logic is required in the code to obtain the default value, you can set the default to null in your config file (`my.config.key = null`), and read an `Option[T]`:
```scala
val myConfigOption: Option[String] = configuration.get[Option[String]]("my.config.key")
val myConfig: String = myConfigOption.getOrElse(computeDefaultValue())
```

Also, there are several methods in the old [`play.api.Configuration`](api/scala/play/api/Configuration.html) that return Java types, like `getBooleanList`. We recommend using the Scala version `get[Seq[Boolean]]` instead if possible. If that is not possible, you can access the `underlying` Config object and call `getBooleanList` from it.

The deprecation messages on the existing methods also explain how to migrate each method. See [[the Scala Configuration docs|ScalaConfig]] for more details on the proper use of [`play.api.Configuration`](api/scala/play/api/Configuration.html).

## Play JSON API changes

### JSON array index lookup

If you are using the Scala play-json API, there was a small change in the way the `JsLookup` implicit class works. For example, if you have code like:

```scala
val bar = (jsarray(index) \ "bar").as[Bar]
```
where `index` is an array index and `jsarray` is a `JsArray`, now you should write:
```scala
val bar = (jsarray \ index \ "bar").as[Bar]
```

This was done to bring the behavior of indexing on `JsArray`s in line with that of other collections in Scala. Now the `jsarray(index)` method will return the value at the index, throwing an exception if it does not exist.

## Removed APIs

### Removed Crypto API

The Crypto API has removed the deprecated classes `play.api.libs.Crypto`, `play.libs.Crypto` and `AESCTRCrypter`.  The CSRF references to `Crypto` have been replaced by `CSRFTokenSigner`.  The session cookie references to `Crypto` have been replaced with `CookieSigner`.  Please see [[CryptoMigration25]] for more information.

### `Akka` deprecated methods removed

The deprecated static methods `play.libs.Akka.system` and `play.api.libs.concurrent.Akka.system` were removed.  Use dependency injection to get an instance of `ActorSystem` and access to the actor system.

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

Also, Play 2.6.x now uses the Akka 2.5.x release series. Read Akka [migration guide from 2.4.x to 2.5.x](https://doc.akka.io/docs/akka/current/project/migration-guide-2.4.x-2.5.x.html?language=scala) to see how to adapt your own code if necessary.

### Removed Yaml API

We removed `play.libs.Yaml` since there was no use of it inside of play anymore. If you still need support for the Play YAML integration you need to add `snakeyaml` in you `build.sbt`:

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

Or in Scala:

```scala
class Yaml @Inject()(environment: play.api.Environment) {
  def load(resourceName: String) = {
    load(environment.resourceAsStream(resourceName), environment.classLoader)
  }

  def load(inputStream: InputStream, classLoader: ClassLoader) = {
    new org.yaml.snakeyaml.Yaml(new CustomClassLoaderConstructor(classloader)).load(inputStream)
  }
}
```

If you explicitly depend on an alternate DI library for Play, or have defined your own custom application loader, no changes should be required.

Libraries that provide Play DI support should define the `play.application.loader` configuration key. If no external DI library is provided, Play will refuse to start unless you point that to an [`ApplicationLoader`](api/scala/play/api/ApplicationLoader.html).

### Removed deprecated `play.Routes`

The deprecated `play.Routes` class used to create a JavaScript router were removed. You now have to use the new Java or Scala helpers:

* [[Javascript Routing in Scala|ScalaJavascriptRouting]]
* [[Javascript Routing in Java|JavaJavascriptRouter]]

## Removed libraries

In order to make the default play distribution a bit smaller we removed some libraries. The following libraries are no longer dependencies in Play 2.6, so you will need to manually add them to your build if you use them.

### Joda-Time removal

We recommend using the `java.time` APIs, so we are removing joda-time support from the core of Play.

Play's Scala forms library had some Joda formats. If you don't wish to migrate, you can add the `jodaForms` module in your `build.sbt`:

```scala
libraryDependencies += jodaForms
```

And then import the corresponding object:

```scala
import play.api.data.JodaForms._
```

If you need Joda support in play-json, you can add the following dependency:

```scala
libraryDependencies += "com.typesafe.play" %% "play-json-joda" % playJsonVersion
```

where `playJsonVersion` is the play-json version you wish to use. Play 2.6.x should be compatible with play-json 2.6.x. Note that play-json is now a separate project (described later).

```scala
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
```

### Joda-Convert removal

Play had some internal uses of `joda-convert` if you used it in your project you need to add it to your `build.sbt`:

```scala
libraryDependencies += "org.joda" % "joda-convert" % "1.8.1"
```

### XercesImpl removal

For XML handling Play used the Xerces XML Library. Since modern JVM are using Xerces as a reference implementation we removed it. If your project relies on the external package you can simply add it to your `build.sbt`:

```scala
libraryDependencies += "xerces" % "xercesImpl" % "2.11.0"
```

### H2 removal

Prior versions of Play prepackaged the H2 database. But to make the core of Play smaller we removed it. If you make use of H2 you can add it to your `build.sbt`:

```scala
libraryDependencies += "com.h2database" % "h2" % "1.4.193"
```

If you only used it in your test you can also just use the `Test` scope:

```scala
libraryDependencies += "com.h2database" % "h2" % "1.4.193" % Test
```

The [[H2 Browser|Developing-with-the-H2-Database#H2-Browser]] will still work after you added the dependency.

### snakeyaml removal

Play removed `play.libs.Yaml` and therefore the dependency on `snakeyaml` was dropped. If you still use it add it to your `build.sbt`:

```scala
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
```

See also [notes about the removal of Yaml API](#Removed-Yaml-API).

### Tomcat-servlet-api removal

Play removed the `tomcat-servlet-api` since it was of no use. If you still use it add it to your `build.sbt`:

```scala
libraryDependencies += "org.apache.tomcat" % "tomcat-servlet-api" % "8.0.33"
```

### fork-run removal

The `sbt-fork-run-plugin` will no longer be generated, as it was only needed for the now end-of-life activator utility. As it will no longer resolve for 2.6 it can safely be removed altogether. 

## Request attributes

All request objects now contain *attributes*. Request attributes are a replacement for request *tags*. Tags have now been deprecated and you should upgrade to attributes. Attributes are more powerful than tags; you can use attributes to store objects in requests, whereas tags only supported storing Strings.

### Request tags deprecation

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
  val UserName: TypedKey[String] = TypedKey("userName")
}
// Getting an attribute from a Request or RequestHeader
val userName: String = req.attrs(Attrs.UserName)
val optUserName: [String] = req.attrs.get(Attrs.UserName)
// Setting an attribute on a Request or RequestHeader
val newReq = req.addAttr(Attrs.UserName, newName)
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
  val UserName: TypedKey[User] = TypedKey("user")
}
```

### Calling `FakeRequest.withCookies` no longer updates the `Cookies` header

Request cookies are now stored in a request attribute. Previously they were stored in the request's [`Cookie`](api/scala/play/api/mvc/Cookie.html) header `String`. This required encoding and decoding the cookie to the header whenever the cookie changed.

Now that cookies are stored in request attributes updating the cookie will change the new cookie attribute but not the [`Cookie`](api/scala/play/api/mvc/Cookie.html) HTTP header. This will only affect your tests if you're relying on the fact that calling `withCookies` will update the header.

If you still need the old behavior you can still use [`Cookies.encodeCookieHeader`](api/scala/play/api/mvc/Cookies$.html#encodeCookieHeader\(cookies:Seq[play.api.mvc.Cookie]\):String) to convert the [`Cookie`](api/scala/play/api/mvc/Cookie.html) objects into an HTTP header then store the header with `FakeRequest.withHeaders`.

### `play.api.mvc.Security.username` (Scala API), `session.username` changes

`play.api.mvc.Security.username` (Scala API), `session.username` config key and dependent actions helpers are deprecated. `Security.username` just retrieves the `session.username` key from configuration, which defined the session key used to get the username. It was removed since it required statics to work, and it's fairly easy to implement the same or similar behavior yourself.

You can read the username session key from configuration yourself using `configuration.get[String]("session.username")`.

If you're using the `Authenticated(String => EssentialAction)` method, you can easily create your own action to do something similar:

```scala
def AuthenticatedWithUsername(action: String => EssentialAction) =
  WithAuthentication[String](_.session.get(UsernameKey))(action)
```

where `UsernameKey` represents the session key you want to use for the username.

### Request Security (Java API) username property is now an attribute

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

### Router tags are now attributes

If you used any of the `Router.Tags.*` tags, you should change your code to use the new [`Router.Attrs.HandlerDef`](api/scala/play/api/routing/Router$$Attrs$.html#HandlerDef:play.api.libs.typedmap.TypedKey[play.api.routing.HandlerDef]) (Scala) or [`Router.Attrs.HANDLER_DEF`](api/java/play/routing/Router.Attrs.html#HANDLER_DEF) (Java) attribute instead. The existing tags are still available, but are deprecated and will be removed in a future version of Play.

This new attribute contains a `HandlerDef` object with all the information that is currently in the tags. The current tags all correspond to a field in the `HandlerDef` object:

| Java tag name         | Scala tag name      | `HandlerDef` method |
|:----------------------|:--------------------|:--------------------|
| `ROUTE_PATTERN`       | `RoutePattern`      | `path`              |
| `ROUTE_VERB`          | `RouteVerb`         | `verb`              |
| `ROUTE_CONTROLLER`    | `RouteController`   | `controller`        |
| `ROUTE_ACTION_METHOD` | `RouteActionMethod` | `method`            |
| `ROUTE_COMMENTS`      | `RouteComments`     | `comments`          |

> **Note**: As part of this change the `HandlerDef` object has been moved from the `play.core.routing` internal package into the `play.api.routing` public API package.

## `play.api.libs.concurrent.Execution` is deprecated

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

The recommended way to create a custom execution context is through [`CustomExecutionContext`](api/scala/play/api/libs/concurrent/CustomExecutionContext.html), which uses the Akka dispatcher system ([java](https://doc.akka.io/docs/akka/2.5/dispatchers.html?language=java) / [scala](https://doc.akka.io/docs/akka/2.5/dispatchers.html?language=scala))  so that executors can be defined through configuration.

To use your own execution context, extend the [`CustomExecutionContext`](api/scala/play/api/libs/concurrent/CustomExecutionContext.html) abstract class with the full path to the dispatcher in the `application.conf` file:

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

Please see [[ThreadPools]] page for more information on custom thread pool configuration, and [[JavaAsync]] / [[ScalaAsync]] for using `CustomExecutionContext`.

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

The `play.api.libs.MimeTypes` class has been changed to [`play.api.http.FileMimeTypes`](api/scala/play/api/http/FileMimeTypes.html) interface, and the implementation has changed to [`play.api.http.DefaultFileMimeTypes`](api/scala/play/api/http/DefaultFileMimeTypes.html).

All the results that send files or resources now take `FileMimeTypes` implicitly, i.e.

```scala
implicit val fileMimeTypes: FileMimeTypes = ...
Ok(file) // <-- takes implicit FileMimeTypes
```

An implicit instance of `FileMimeTypes` is provided by `BaseController` (and its subclass `AbstractController` and subtrait `InjectedController`) through the `ControllerComponents` class, to provide a convenient binding:

```scala
class SendFileController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

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

## Default Filters

Play now comes with a default set of enabled filters, defined through configuration.  If the property `play.http.filters` is null, then the default is now [`play.api.http.EnabledFilters`](api/scala/play/api/http/EnabledFilters.html), which loads up the filters defined by fully qualified class name in the `play.filters.enabled` configuration property.

In Play itself, `play.filters.enabled` is an empty list.  However, the filters library is automatically loaded in sbt as an AutoPlugin called `PlayFilters`, and will append the following values to the `play.filters.enabled` property:

* [`play.filters.csrf.CSRFFilter`](api/scala/play/filters/csrf/CSRFFilter.html)
* [`play.filters.headers.SecurityHeadersFilter`](api/scala/play/filters/headers/SecurityHeadersFilter.html)
* [`play.filters.hosts.AllowedHostsFilter`](api/scala/play/filters/hosts/AllowedHostsFilter.html)

This means that on new projects, CSRF protection ([[ScalaCsrf]] / [[JavaCsrf]]), [[SecurityHeaders]] and [[AllowedHostsFilter]] are all defined automatically.

### Effects of Default Filters

The default filters are configured to give a "secure by default" configuration to projects. 

**You should keep these filters enabled: they make your application more secure.** 

If you did not have these filters enabled in an existing project, then there is some configuration required, and you may not be familiar with the errors and failures involved.  To help with migration, we'll go over each filter, what it does and what configuration is required.

#### CSRFFilter 

The CSRF filter is described in [[ScalaCsrf]] and [[JavaCsrf]].  It protects against [cross site request forgery](https://en.wikipedia.org/wiki/Cross-site_request_forgery) attacks, by adding a CSRF token to forms that is checked on POST requests.

##### Why it is enabled by default

CSRF is a very common attack that takes very little skill to implement.  You can see an example of a CSRF attack using Play at [https://github.com/Manc/play-scala-csrf](https://github.com/Manc/play-scala-csrf).  

##### What changes do I need to make? 

If you are migrating from an existing project that does not use CSRF form helpers such as `CSRF.formField`, then you may see "403 Forbidden" on PUT and POST requests from the CSRF filter.  

Adding `CSRF.formField` to your form templates will resolve the error If you are making requests with AJAX, you can place the CSRF token in the HTML page, and then add it to the request using the `Csrf-Token` header.

To check this behavior, please add `<logger name="play.filters.csrf" value="TRACE"/>` to your `logback.xml`.

You may also want to enable SameSite cookies in Play, which provide an additional defense against CSRF attacks.

#### SecurityHeadersFilter

[[SecurityHeadersFilter|SecurityHeaders]] prevents [cross site scripting](https://en.wikipedia.org/wiki/Cross-site_scripting) and [clickjacking](https://en.wikipedia.org/wiki/Clickjacking) attacks, by adding extra HTTP headers to the request.  

##### Why it is enabled by default

Browser based attacks are extremely common, and security headers can provide a defense in depth to help frustrate those attacks.

##### What changes do I need to make? 

The default "Content-Security-Policy" settings are quite strict, and it is likely that you will need to experiment with it to find the most useful settings.  The Content-Security-Policy settings will change how Javascript and remote frames are displayed in a browser.  **Embedded Javascript or CSS will not be loaded in your web page until you modify the Content-Security-Policy header.** 

If you are sure that you do not want to enable it, you can disable the Content-Security-Policy as follows:

```
play.filters.headers.contentSecurityPolicy=null
```

[CSP-Useful](https://github.com/nico3333fr/CSP-useful) is a good resource on Content-Security-Policy in general.  Note that there are other potential solutions to embedded Javascript, such as adding a custom CSP nonce on every request.

The other headers are less intrusive, and are unlikely to cause problems on a plain website, but may cause cookie or rendering problems on a Single Page Application.  Mozilla has documentation describing each header in detail, using the header name in the URL: for example, for X-Frame-Options go to [https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options).


```
play.filters.headers {

    # The X-Frame-Options header. If null, the header is not set.
    frameOptions = "DENY"

    # The X-XSS-Protection header. If null, the header is not set.
    xssProtection = "1; mode=block"

    # The X-Content-Type-Options header. If null, the header is not set.
    contentTypeOptions = "nosniff"

    # The X-Permitted-Cross-Domain-Policies header. If null, the header is not set.
    permittedCrossDomainPolicies = "master-only"

    # The Content-Security-Policy header. If null, the header is not set.
    contentSecurityPolicy = "default-src 'self'"

    # The Referrer-Policy header. If null, the header is not set.
    referrerPolicy = "origin-when-cross-origin, strict-origin-when-cross-origin"

    # If true, allow an action to use .withHeaders to replace one or more of the above headers
    allowActionSpecificHeaders = false
}
```

#### AllowedHostsFilter

The AllowedHostsFilter adds a whitelist of allowed hosts and sends a 400 (Bad Request) response to all requests with a host that do not match the whitelist.

##### Why it is enabled by default

This is an important filter to use in development, because DNS rebinding attacks can be used against a developer’s instance of Play: see [Rails Webconsole DNS Rebinding](https://benmmurphy.github.io/blog/2016/07/11/rails-webconsole-dns-rebinding/) for an example of how short lived DNS rebinding can attack a server running on localhost.

##### What changes do I need to make? 

If you are running a Play application on something other than localhost, you must configure the AllowedHostsFilter to specifically allow the hostname/ip you are connecting from.  This is especially important to note when you change environments, because typically you'll run on localhost in development, but will run remotely in staging and production.

```
play.filters.hosts {
  # Allow requests to example.com, its subdomains, and localhost:9000.
  allowed = [".example.com", "localhost:9000"]
}
```

### Appending To Filters

To append to the defaults list, use the `+=`:

```
play.filters.enabled+=MyFilter
```

If you have defined your own filters by extending `play.api.http.DefaultHttpFilters`, then you can also combine `EnabledFilters` with your own list in code, so if you have previously defined projects, they still work as usual:

```scala
class Filters @Inject()(enabledFilters: EnabledFilters, corsFilter: CORSFilter)
  extends DefaultHttpFilters(enabledFilters.filters :+ corsFilter: _*)
```

### Testing Default Filters

Because there are several filters enabled, functional tests may need to change slightly to ensure that all the tests pass and requests are valid.  For example, a request that does not have a `Host` HTTP header set to `localhost` will not pass the AllowedHostsFilter and will return a 400 Forbidden response instead.

#### Testing with AllowedHostsFilter

Because the AllowedHostsFilter filter is added automatically, functional tests need to have the Host HTTP header added.

If you are using `FakeRequest` or `Helpers.fakeRequest`, then the `Host` HTTP header is added for you automatically.  If you are using `play.mvc.Http.RequestBuilder`, then you may need to add your own line to add the header manually:

```java
RequestBuilder request = new RequestBuilder()
        .method(GET)
        .header(HeaderNames.HOST, "localhost")
        .uri("/xx/Kiwi");
```

#### Testing with CSRFFilter

Because the CSRFFilter filter is added automatically, tests that render a Twirl template that includes `CSRF.formField`, i.e.

```scala
@(userForm: Form[UserData])(implicit request: RequestHeader, m: Messages)

<h1>user form</h1>

@request.flash.get("success").getOrElse("")

@helper.form(action = routes.UserController.userPost()) {
  @helper.CSRF.formField
  @helper.inputText(userForm("name"))
  @helper.inputText(userForm("age"))
  <input type="submit" value="submit"/>
}
```

must contain a CSRF token in the request.  In the Scala API, this is done by importing `play.api.test.CSRFTokenHelper._`, which enriches `play.api.test.FakeRequest` with the `withCSRFToken` method:

```scala
import play.api.test.CSRFTokenHelper._

class UserControllerSpec extends PlaySpec with GuiceOneAppPerTest {
  "UserController GET" should {

    "render the index page from the application" in {
      val controller = app.injector.instanceOf[UserController]
      val request = FakeRequest().withCSRFToken
      val result = controller.userGet().apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
    }
  }
}
```

In the Java API, this is done by calling `CSRFTokenHelper.addCSRFToken` on a `play.mvc.Http.RequestBuilder` instance:

```
requestBuilder = CSRFTokenHelper.addCSRFToken(requestBuilder);
```

### Disabling Default Filters

The simplest way to disable the default filters is to set the list of filters manually in `application.conf`:

```
play.filters.enabled=[]
```

This may be useful if you have functional tests that you do not want to go through the default filters.

If you want to remove all filter classes, you can disable it through the `disablePlugins` mechanism:

```
lazy val root = project.in(file(".")).enablePlugins(PlayScala).disablePlugins(PlayFilters)
```

or by replacing `EnabledFilters`:

```
play.http.filters=play.api.http.NoHttpFilters
```

If you are writing functional tests involving `GuiceApplicationBuilder` and you want to disable default filters, then you can disable all or some of the filters through configuration by using `configure`:

```scala
GuiceApplicationBuilder().configure("play.http.filters" -> "play.api.http.NoHttpFilters")
```

## Compile Time Default Filters

If you are using compile time dependency injection, then the default filters are resolved at compile time, rather than through runtime.

This means that the `BuiltInComponents` trait now contains an `httpFilters` method which is left abstract:

```scala
trait BuiltInComponents {

  /** A user defined list of filters that is appended to the default filters */
  def httpFilters: Seq[EssentialFilter]
}
```

The default list of filters is defined in `play.filters.HttpFiltersComponents`:

```scala
trait HttpFiltersComponents
     extends CSRFComponents
     with SecurityHeadersComponents
     with AllowedHostsComponents {

   def httpFilters: Seq[EssentialFilter] = Seq(csrfFilter, securityHeadersFilter, allowedHostsFilter)
}
```

In most cases you will want to mixin HttpFiltersComponents and append your own filters:

```scala
class MyComponents(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context)
  with play.filters.HttpFiltersComponents {

  lazy val loggingFilter = new LoggingFilter()
  override def httpFilters = {
    super.httpFilters :+ loggingFilter
  }
}
```

If you want to filter elements out of the list, you can do the following:

```scala
class MyComponents(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context)
  with play.filters.HttpFiltersComponents {
  override def httpFilters = {
    super.httpFilters.filterNot(_.getClass == classOf[CSRFFilter])
  }
}
```

### Disabling Compile Time Default Filters

To disable the default filters, mixin `play.api.NoHttpFiltersComponents`:

```scala
class MyComponents(context: ApplicationLoader.Context)
   extends BuiltInComponentsFromContext(context)
   with NoHttpFiltersComponents
   with AssetsComponents {

  lazy val homeController = new HomeController(controllerComponents)
  lazy val router = new Routes(httpErrorHandler, homeController, assets)
}
```

## JWT Support

Play's cookie encoding has been switched to use JSON Web Token (JWT) under the hood.  JWT comes with a number of advantages, notably automatic signing with HMAC-SHA-256, and support for automatic "not before" and "expires after" date checks which ensure the session cookie cannot be reused outside of a given time window.

More information is available under [[Configuring the Session Cookie|SettingsSession]] page.

### Fallback Cookie Support

Play's cookie encoding uses a "fallback" cookie encoding mechanism that reads in JWT encoded cookies, then attempts reading a URL encoded cookie if the JWT parsing fails, so you can safely migrate existing session cookies to JWT.  This functionality is in the `FallbackCookieDataCodec` trait and leveraged by `DefaultSessionCookieBaker` and `DefaultFlashCookieBaker`.

### Legacy Support

Using JWT encoded cookies should be seamless, but if you want, you can revert back to URL encoded cookie encoding by switching to `play.api.mvc.LegacyCookiesModule` in application.conf file:

```
play.modules.disabled+="play.api.mvc.CookiesModule"
play.modules.enabled+="play.api.mvc.LegacyCookiesModule"
```

### Custom CookieBakers

If you have custom cookies being used in Play, using the `CookieBaker[T]` trait, then you will need to specify what kind of encoding you want for your custom cookie baker.

The `encode` and `decode` methods that `Map[String, String]` to and from the format found in the browser have been extracted into `CookieDataCodec`.  There are three implementations: `FallbackCookieDataCodec`, `JWTCookieDataCodec`, or `UrlEncodedCookieDataCodec`, which respectively represent URL-encoded with an HMAC, or a JWT, or a "read signed or JWT, write JWT" codec.

You will also need to provide a `JWTConfiguration` case class, using the `JWTConfigurationParser` with the path to your configuration, or use `JWTConfiguration()` for the defaults.


```scala
@Singleton
class UserInfoCookieBaker @Inject()(service: UserInfoService,
                                    val secretConfiguration: SecretConfiguration)
  extends CookieBaker[UserInfo] with JWTCookieDataCodec {

  override val COOKIE_NAME: String = "userInfo"

  override val isSigned = true

  override def emptyCookie: UserInfo = new UserInfo()

  override protected def serialize(userInfo: UserInfo): Map[String, String] = service.encrypt(userInfo)

  override protected def deserialize(data: Map[String, String]): UserInfo = service.decrypt(data)

  override val path: String = "/"

  override val jwtConfiguration: JWTConfiguration = JWTConfigurationParser()
}
```

## Deprecated Futures methods

The following `play.libs.concurrent.Futures` static methods have been deprecated:

* `timeout(A value, long amount, TimeUnit unit)`
* `timeout(final long delay, final TimeUnit unit)`
* `delayed(Supplier<A> supplier, long delay, TimeUnit unit, Executor executor)`

A dependency injected instance of `Futures` should be used instead:

```java
class MyClass {
    @Inject
    public MyClass(play.libs.concurrent.Futures futures) {
        this.futures = futures;
    }

    CompletionStage<Double> callWithOneSecondTimeout() {
        return futures.timeout(computePIAsynchronously(), Duration.ofSeconds(1));
    }
}
```

## Updated libraries

### Netty 4.1

Netty was upgraded to [version 4.1](https://netty.io/news/2016/05/26/4-1-0-Final.html). This was possible mainly because version 4.0 was shaded by [[play-ws migration to a standalone module|WSMigration26]]. So, if you are using [[Netty Server|NettyServer]] and some library that depends on Netty 4.0, we recommend that you try to upgrade to a newer version of the library, or you can start to use the [[Akka Server|AkkaHttpServer]].

And if you are, for some reason, directly using Netty classes, you should [adapt your code to this new version](https://netty.io/wiki/new-and-noteworthy-in-4.1.html).

### FluentLenium and Selenium

The FluentLenium library was updated to version 3.2.0 and Selenium was updated to version [3.3.1](https://seleniumhq.wordpress.com/2016/10/13/selenium-3-0-out-now/) (you may want to see the [changelog here](https://raw.githubusercontent.com/SeleniumHQ/selenium/master/java/CHANGELOG)). If you were using Selenium's WebDriver API before, there should not be anything to do. Please check [this](https://seleniumhq.wordpress.com/2016/10/04/selenium-3-is-coming/) announcement for further information.
If you were using the FluentLenium library you might have to change some syntax to get your tests working again. Please see FluentLenium's [Migration Guide](http://fluentlenium.org/migration/from-0.13.2-to-1.0-or-3.0/) for more details about how to adapt your code.

### HikariCP

HikariCP was updated and a new configuration was introduced: `initializationFailTimeout`. This new configuration should be used to replace `initializationFailFast` which is now deprecated. See [HikariCP changelog](https://github.com/brettwooldridge/HikariCP/blob/dev/CHANGES) and [documentation for `initializationFailTimeout`](https://github.com/brettwooldridge/HikariCP#infrequently-used) to better understand how to use this new configuration.

## Other Configuration changes

There are some configuration changes. The old configuration paths will generally still work, but a deprecation warning will be output at runtime if you use them. Here is a summary of the changed keys:

| Old key                       | New key                                 |
|-------------------------------|-----------------------------------------|
| `play.crypto.secret`          | `play.http.secret.key`                  |
| `play.crypto.provider`        | `play.http.secret.provider`             |
| `play.websocket.buffer.limit` | `play.server.websocket.frame.maxLength` |
