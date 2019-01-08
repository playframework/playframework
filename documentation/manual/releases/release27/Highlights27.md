<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# What's new in Play 2.7

This page highlights the new features of Play 2.7. If you want to learn about the changes you need to make when you migrate to Play 2.7, check out the [[Play 2.7 Migration Guide|Migration27]].

## Lifecycle managed by Akka's Coordinated Shutdown

Play 2.6 introduced the usage of Akka's [Coordinated Shutdown](https://doc.akka.io/docs/akka/2.5/actors.html?language=scala#coordinated-shutdown) but still didn't use it all across the core framework or exposed it to the end user. Coordinated Shutdown is an Akka Extension with a registry of tasks that can be run in an ordered fashion during the shutdown of the Actor System.

Coordinated Shutdown internally handles Play 2.7 Play's lifecycle and an instance of `CoordinatedShutdown` is available for injection. Coordinated Shutdown gives you fine grained phases - organized as a [directed acyclic graph (DAG)](https://en.wikipedia.org/wiki/Directed_acyclic_graph) - where you can register tasks instead of just having a single phase like Play's application lifecycle. For example, you can add tasks to run before or after server binding, or after all the current requests finishes. Also, you will have better integration with [Akka Cluster](https://doc.akka.io/docs/akka/2.5/common/cluster.html).

You can find more details on the new section on [[Coordinated Shutdown on the Play manual|Shutdown]], or you can have a look at Akka's [reference docs on Coordinated Shutdown](https://doc.akka.io/docs/akka/2.5/actors.html?languages=scala#coordinated-shutdown).

## Guice was upgraded to 4.2.2

Guice, the default dependency injection framework used by Play, was upgraded to 4.2.2 (from 4.1.0). Have a look at the [4.2.2](https://github.com/google/guice/wiki/Guice422), [4.2.1](https://github.com/google/guice/wiki/Guice421) and the [4.2.0](https://github.com/google/guice/wiki/Guice42) release notes. This new Guice version introduces breaking changes, so make sure you check the [[Play 2.7 Migration Guide|Migration27]].

## Java forms bind `multipart/form-data` file uploads

Until Play 2.6, the only way to retrieve a file that was uploaded via a `multipart/form-data` encoded form was [[by calling|JavaFileUpload#Uploading-files-in-a-form-using-multipart/form-data]] `request.body().asMultipartFormData().getFile(...)` inside the action method.

Starting with Play 2.7 such an uploaded file will now also be bound to a Java Form. If you are *not* using a [[custom multipart file part body parser|JavaFileUpload#Writing-a-custom-multipart-file-part-body-parser]] all you need to do is add a `FilePart` of type `TemporaryFile` to your form:

```java
import play.libs.Files.TemporaryFile;
import play.mvc.Http.MultipartFormData.FilePart;

public class MyForm {

  private FilePart<TemporaryFile> myFile;
	
  public void setMyFile(final FilePart<TemporaryFile> myFile) {
    this.myFile = myFile;
  }

  public FilePart<TemporaryFile> getMyFile() {
    return this.myFile;
  }
}
```

[[Like before|JavaForms#Defining-a-form]], use the [`FormFactory`](api/java/play/data/FormFactory.html) you injected into your Controller to create the form:

```java
Form<MyForm> form = formFactory.form(MyForm.class).bindFromRequest(req);
```

If the binding was successful (form validation passed) you can access the file:

```java
MyForm myform = form.get();
myform.getMyFile();
```

Some useful methods were added as well to work with uploaded files:

```java
// Get all files of the form
form.files();

// Access the file of a Field instance
Field myFile = form.field("myFile");
field.file();

// To access a file of a DynamicForm instance
dynamicForm.file("myFile");
```

> **Note:** If you are using using a [[custom multipart file part body parser|JavaFileUpload#Writing-a-custom-multipart-file-part-body-parser]] you just have to replace `TemporaryFile` with the type your body parser uses.

## Constraint annotations offered for Play Java are now @Repeatable

All of the constraint annotations defined by `play.data.validation.Constraints` are now `@Repeatable`. This change lets you, for example, reuse the same annotation on the same element several times but each time with different `groups`. For some constraints however it makes sense to let them repeat itself anyway, like `@ValidateWith`:

```java
@Validate(groups={GroupA.class})
@Validate(groups={GroupB.class})
public class MyForm {

    @ValidateWith(MyValidator.class)
    @ValidateWith(MyOtherValidator.class)
    @Pattern(value="[a-k]", message="Should be a - k")
    @Pattern(value="[c-v]", message="Should be c - v")
    @MinLength(value=4, groups={GroupA.class})
    @MinLength(value=7, groups={GroupB.class})
    private String name;

    //...
}
```

You can of course also make your own custom constraints `@Repeatable` as well and Play will automatically recognize that.

## Payloads for Java `validate` and `isValid` methods

When using [[advanced validation features|JavaForms#Advanced-validation]] you can now pass a `ValidationPayload` object, containing useful information sometimes needed for a validation process, to a Java `validate` or `isValid` method.
To pass such a payload to a `validate` method just annotate your form with `@ValidateWithPayload` (instead of just `@Validate`) and implement `ValidatableWithPayload` (instead of just `Validatable`):

```java
import java.util.Map;
import com.typesafe.config.Config;
import play.data.validation.Constraints.ValidatableWithPayload;
import play.data.validation.Constraints.ValidateWithPayload;
import play.data.validation.Constraints.ValidationPayload;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.typedmap.TypedMap;

@ValidateWithPayload
public class SomeForm implements ValidatableWithPayload<String> {

    @Override
    public String validate(ValidationPayload payload) {
        Lang lang = payload.getLang();
        Messages messages = payload.getMessages();
        Map<String, Object> ctxArgs = payload.getArgs();
        TypedMap attrs = payload.getAttrs();
        Config config = payload.getConfig();
        // ...
    }

}
```

In case you wrote your own [[custom class-level constraint|JavaForms#Custom-class-level-constraints-with-DI-support]], you can also pass a payload to an `isValid` method by implementing `PlayConstraintValidatorWithPayload` (instead of just `PlayConstraintValidator`):

```java
import javax.validation.ConstraintValidatorContext;

import play.data.validation.Constraints.PlayConstraintValidatorWithPayload;
import play.data.validation.Constraints.ValidationPayload;
// ...

public class ValidateWithDBValidator implements PlayConstraintValidatorWithPayload<SomeValidatorAnnotation, SomeValidatableInterface<?>> {

    //...

    @Override
    public boolean isValid(final SomeValidatableInterface<?> value, final ValidationPayload payload, final ConstraintValidatorContext constraintValidatorContext) {
        // You can now pass the payload on to your custom validate(...) method:
        return reportValidationStatus(value.validate(...., payload), constraintValidatorContext);
    }

}
```

> **Note:** Don't get confused with `ValidationPayload` and `ConstraintValidatorContext`: The former class is provided by Play and is what you use in your day-to-day work when dealing with forms in Play. The latter class is defined by the [Bean Validation specification](https://beanvalidation.org/2.0/spec/) and is used only internally in Play - with one exception: This class emerges when your write your own custom class-level constraints, like in the last example above, where you only need to pass it on to the `reportValidationStatus` method however anyway.

## Support for Caffeine

Play now offers a CacheApi implementation based on [Caffeine](https://github.com/ben-manes/caffeine/). Caffeine is the recommended cache implementation for Play users.

To migrate from EhCache to Caffeine you will have to remove `ehcache` from your dependencies and replace it with `caffeine`. To customize the settings from the defaults, you will also need to update the configuration in application.conf as explained in the documentation.

Read the documentation for the [[Java cache API|JavaCache]] and [[Scala cache API|ScalaCache]] to learn more about configuring caching with Play.

## New Content Security Policy Filter

There is a new [[Content Security Policy filter|CspFilter]] available that supports CSP nonce and hashes for embedded content.

The previous setting of enabling CSP by default and setting it to `default-src 'self'` was too strict, and interfered with plugins.  The CSP filter is not enabled by default, and the `contentSecurityPolicy` in the [[SecurityHeaders filter|SecurityHeaders]] is now deprecated and set to `null` by default.

The CSP filter uses Google's [Strict CSP policy](https://csp.withgoogle.com/docs/strict-csp.html) by default, which is a nonce based policy.  It is recommended to use this as a starting point, and use the included CSPReport body parsers and actions to log CSP violations before enforcing CSP in production.

## HikariCP upgraded

[HikariCP](https://github.com/brettwooldridge/HikariCP) was updated to its latest major version. Have a look at the [[Migration Guide|Migration27#HikariCP-update]] to see what changed.

## Play WS `curl` filter for Java

Play WS enables you to create `play.libs.ws.WSRequestFilter` to inspect or enrich the requests made. Play provides a "log as `curl`" filter, but this was lacking for Java developers. You can now write something like:

```java
ws.url("https://www.playframework.com")
  .setRequestFilter(new AhcCurlRequestLogger())
  .addHeader("My-Header", "Header value")
  .get();
```

And then the following log will be printed:

```bash
curl \
  --verbose \
  --request GET \
  --header 'My-Header: Header Value' \
  'https://www.playframework.com'
```

This can be specially useful if you want to reproduce the request in isolation and also change `curl` parameters to see how it goes.

## Gzip Filter now supports compression level configuration

When using [[gzip encoding|GzipEncoding]], you can now configure the compression level to use. You can configure it using `play.filters.gzip.compressionLevel`, for example:

```
play.filters.gzip.compressionLevel = 9
```

See more details at [[GzipEncoding]].

## API Additions

Here are some of the relevant API additions we made for Play 2.7.0.

### Result `HttpEntity` streamed methods

Previous versions of Play had convenient methods to stream results using HTTP chunked transfer encoding:

Java
: ```java
public Result chunked() {
  Source<ByteString, NotUsed> body = Source.from(Arrays.asList(ByteString.fromString("first"), ByteString.fromString("second")));
  return ok().chunked(body);
}
```

Scala
: ```scala
def chunked = Action {
  val body = Source(List("first", "second", "..."))
  Ok.chunked(body)
}
```

In Play 2.6, there was no convenient method to return a streamed Result in the same way without using HTTP chunked encoding. You instead had to write this:

Java
: ```java
public Result streamed() {
  Source<ByteString, NotUsed> body = Source.from(Arrays.asList(ByteString.fromString("first"), ByteString.fromString("second")));
  return ok().sendEntity(new HttpEntity.Streamed(body, Optional.empty(), Optional.empty()));
}
```

Scala
: ```scala
def streamed = Action {
  val body = Source(List("first", "second", "...")).map(s => ByteString.fromString(s))
  Ok.sendEntity(HttpEntity.Streamed(body, None, None))
}
```

Play 2.7 fixes this by adding a new `streamed` method on results, that works similar to `chunked`:

Java
: ```java
public Result streamed() {
  Source<ByteString, NotUsed> body = Source.from(Arrays.asList(ByteString.fromString("first"), ByteString.fromString("second")));
  return ok().streamed(body, Optional.empty(), Optional.empty());
}
```

Scala
: ```scala
def streamed = Action {
  val body = Source(List("first", "second", "...")).map(s => ByteString.fromString(s))
  Ok.streamed(body, contentLength = None)
}
```

## New Http Error Handlers

Play 2.7 brings two new implementations for `play.api.http.HttpErrorHandler`. The first one is [`JsonHttpErrorHandler`](api/scala/play/api/http/JsonHttpErrorHandler.html), which will return errors formatted in JSON and is a better alternative if you are developing an REST API that accepts and returns JSON payloads. The second one is [`HtmlOrJsonHttpErrorHandler`](api/scala/play/api/http/HtmlOrJsonHttpErrorHandler.html) which returns HTML or JSON errors based on the preferences specified in client's `Accept` header. It is a better option if your application uses a mixture of HTML and JSON, as is common in modern web apps.

You can read more details at the docs for [[Java|JavaErrorHandling]] or [[Scala|ScalaErrorHandling]].

## Nicer syntax for `Router.withPrefix`

In Play 2.7 we introduce some syntax sugar to use `play.api.routing.Router.withPrefix`. Instead of writing:

```scala
val router = apiRouter.withPrefix("/api")
```

You can now write:

```scala
val router = "/api" /: apiRouter
```

Or even combine more path segments:

```scala
val router = "/api" /: "v1" /: apiRouter
```

### Isolation level for Database transactions

You can now choose an isolation level when using `play.api.db.Database.withTransaction` API (`play.db.Database` for Java users). For example:

Java
: ```java
public void someDatabaseOperation() {
  database.withTransaction(TransactionIsolationLevel.ReadUncommitted, connection -> {
        ResultSet resultSet = connection.prepareStatement("select * from users where id = 10").executeQuery();
        // consume the resultSet and return some value
  });
}
```

Scala
: ```scala
def someDatabaseOperation(): Unit = {
  database.withTransaction(TransactionIsolationLevel.ReadUncommitted) { connection =>
        val resultSet: ResultSet = connection.prepareStatement("select * from users where id = 10").executeQuery();
        // consume the resultSet and return some value
  }
}
```

The available transaction isolation levels mimic what is defined in `java.sql.Connection`.
