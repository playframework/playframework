<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Java `Http.Context` changes

`play.mvc.Http.Context` is a crucial part of Java HTTP & MVC APIs, but it is not a good abstraction of how these APIs should work. It either has some concepts that could be better modeled or implementation details that are complex to test and to reason about in a multi-threading framework like Play. For example, `Http.Context` uses a thread local to capture and access the current request, but it gives the impression that the current request can be accessed from any place, which is not currently true if you are using Actors or a custom thread pool.

Regarding the API modeling, there are some duplicated concepts (like `play.mvc.Result` vs `play.mvc.Http.Response`) and some methods look out of the place (for example `play.mvc.Http.Context.id` instead of using `play.mvc.Http.RequestHeader.id`). Given that, multiple changes were made to `Http.Context` and the idea is to move away from it. We are then providing new APIs that are simpler to test, to reason about and to maintain in the future.

Since `play.mvc.Http.Context` is a central part of the existing APIs, deprecating it had an impact on multiple places that were depending on it, take for example `play.mvc.Controller`. This page documents these changes and how to migrate, but you can see the deprecated Javadocs for each methods too.

## `Http.Context.current()` and `Http.Context.request()` deprecated

That means other methods that depend directly on these two were also deprecated:

1. `play.mvc.Controller.ctx()`
1. `play.mvc.Controller.request()`

Before Play 2.7, when using Play with Java, the only way to access the `Http.Request` was `Http.Context.current()` which was used internally by `Controller.request()` method. The problem with `Http.Context.current()` is that it is implemented using a thread local, which is harder to test, to keep in sync with changes made at other places and makes it harder to access the request in other threads.

With Play 2.7 you can now access the current request by just adding it as a param to your routes and actions.

For example, the routes files contain:

```routes
GET     /       controllers.HomeController.index(request: Request)
```

And the corresponding action method:

```java
import play.mvc.*;

public class HomeController extends Controller {

    public Result index(Http.Request request) {
        return ok("Hello, your request path " + request.path());
    }
}
```

Play will automatically detect a route param of type `Request` (which is an import for `play.mvc.Http.Request`) and will pass the actual request into the corresponding action method's param.

> **Note**: It is unlikely but possible that you have a custom `QueryStringBindable` or `PahBindable` with the name `Request`. If so, that one would now collide with Play detection of request params.
> Therefore you should use the fully qualified name of your `Request` type, for example.
>
>     GET    /        controllers.HomeController.index(myRequest: com.mycompany.Request)

If you use `Http.Context.current()` in other places besides controllers you have to pass the desired data via method parameters to these places now. Look at this example which checks if the current request's remote address is on a blacklist:

### Before

```java
import play.mvc.Http;

public class SecurityHelper {

    public static boolean isBlacklisted() {
        String remoteAddress = Http.Context.current().request().remoteAddress();
        return blacklist.contains(remoteAddress);
    }
}
```

Corresponding controller:

```java
import play.mvc.*;

public class HomeController extends Controller {

    public Result index() {
        if (SecurityHelper.isBlacklisted()) {
            return badRequest();
        }
        return ok("Hello, your request path " + request().path());
    }
}
```

### After

```java
public class SecurityHelper {

    public static boolean isBlacklisted(String remoteAddress) {
        return blacklist.contains(remoteAddress);
    }
}
```

Corresponding controller:

```java
import play.mvc.*;

public class HomeController extends Controller {

    public Result index(Http.Request request) {
        if (SecurityHelper.isBlacklisted(request.remoteAddress())) {
            return badRequest();
        }
        return ok("Hello, your request path " + request.path());
    }
}
```

## `Action.call(Context)` deprecated

If you are using [[action composition|JavaActionsComposition]] you have to update your actions to avoid `Http.Context`.

### Before

```java
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class MyAction extends Action.Simple {
    public CompletionStage<Result> call(Http.Context ctx) {
        return delegate.call(ctx);
    }
}
```

### After

```java
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class MyAction extends Action.Simple {
    public CompletionStage<Result> call(Http.Request req) {
        return delegate.call(req);
    }
}
```

## `Http.Context.response()` and `Http.Response` class deprecated

That means other methods that depend directly on these were also deprecated:

1. `play.mvc.Controller.response()`

`Http.Response` was deprecated with other accesses methods to it. It was mainly used to add headers and cookies, but these are already available in `play.mvc.Result` and then the API got a little confused. For Play 2.7, you should migrate code like:

### Before

```java
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Controller;

public class FooController extends Controller {

    // This uses the deprecated response() APIs
    public Result index() {
        response().setHeader("Header", "Value");
        response().setCookie(Http.Cookie.builder("Cookie", "cookie value").build());
        response().discardCookie("CookieName");
        return ok("Hello World");
    }

}
```

### After

The code above should be written as:

```java
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Controller;

public class FooController extends Controller {

    // This uses the deprecated response() APIs
    public Result index() {
        return ok("Hello World")
                .withHeader("Header", "value")
                .withCookies(Http.Cookie.builder("Cookie", "cookie value").build())
                .discardCookie("CookieName");
    }

}
```

If you have action composition that depends on `Http.Context.response`, you can also rewrite it like the code below:

### Before

```java
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class MyAction extends Action.Simple {

    @Override
    public CompletionStage<Result> call(Http.Context ctx) {
        ctx.response().setHeader("Name", "Value");
        return delegate.call(ctx);

    }
}
```

### After

The code above should be written as:

```java
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class MyAction extends Action.Simple {

    @Override
    public CompletionStage<Result> call(Http.Request req) {
        return delegate.call(req)
                .thenApply(result -> result.withHeader("Name", "Value"));
    }
}
```

## Lang and Messages methods in `Http.Context` deprecated

The following methods have been deprecated:

1. `Http.Context.lang()`
1. `Http.Context.changeLang(Lang lang)`
1. `Http.Context.changeLang(String code)`
1. `Http.Context.clearLang()`
1. `Http.Context.setTransientLang(Lang lang)`
1. `Http.Context.setTransientLang(String code)`
1. `Http.Context.clearTransientLang()`
1. `Http.Context.messages()`

That means other methods that depend directly on these were also deprecated:

1. `play.mvc.Controller.lang()`
1. `play.mvc.Controller.changeLang(Lang lang)`
1. `play.mvc.Controller.changeLang(String code)`
1. `play.mvc.Controller.clearLang()`

The new way of changing lang now is to have an instance of [`play.i18n.MessagesApi`](api/java/play/i18n/MessagesApi.html) injected and call corresponding [`play.mvc.Result`](api/java/play/mvc/Result.html) methods. For example:

### Before

```java
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

import play.i18n.MessagesApi;

public class FooController extends Controller {
    public Result action() {
        changeLang(Lang.forCode("es"));
        return Results.ok("Hello");
    }
}
```

### After

```java
import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

import play.i18n.MessagesApi;

public class FooController extends Controller {
    private final MessagesApi messagesApi;

    @Inject
    public FooController(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public Result action() {
        return Results.ok("Hello").withLang(Lang.forCode("es"), messagesApi);
    }
}
```

If you are using `changeLang` to change the `Lang` used to render a template, you should now pass the `Messages` itself as a parameter. This will make the template clearer and easier to read. For example, in an action method, you have to create a `Messages` instance like:

### Before

```java
import play.mvc.Result;
import play.mvc.Controller;

public class MyController extends Controller {
    public Result action() {
        changeLang(Lang.forCode("es"));
        return ok(myview.render(messages));
    }
}
```

### After

```java
import javax.inject.Inject;
import play.i18n.Messages;
import play.i18n.MessagesApi;

import play.mvc.Result;
import play.mvc.Controller;

public class MyController extends Controller {

    private final MessagesApi messagesApi;

    @Inject
    public MyController(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public Result action() {
        Messages messages = this.messagesApi.preferred(Lang.forCode("es"));
        return ok(myview.render(messages));
    }
}
```

Or if you want to have a fallback to the languages of the request you can do that as well:

```java
import javax.inject.Inject;
import play.i18n.Messages;
import play.i18n.MessagesApi;

import play.mvc.Result;
import play.mvc.Controller;

public class MyController extends Controller {

    private final MessagesApi messagesApi;

    @Inject
    public MyController(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public Result action() {
        Lang lang = Lang.forCode("es");

        // Get a Message instance based on the spanish locale, however if that isn't available
        // try to choose the best fitting language based on the current request
        Messages messages = this.messagesApi.preferred(request.withTransientLang(lang));
        return ok(myview.render(messages));
    }
}
```

> **Note**: To not repeat that code again and again inside each action method you could e.g. create the `Messages` instance in an action of the [[action composition chain|JavaActionsComposition]] and save that instance in a request Attribute so you can access it later.

Now the template:

```html
@()(implicit messages: play.i18n.Messages)
@{messages.at("hello")}
```

> **Note**: Declaring `messages` as `implicit` will make it available to sub-views that implicitly require a `MessagesProvider` without the need to pass them one.

And the same applies to `clearLang`:

### Before

```java
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

import play.i18n.MessagesApi;

public class FooController extends Controller {
    public Result action() {
        clearLang();
        return Results.ok("Hello");
    }
}
```

### After

```java
import javax.inject.Inject;

import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

import play.i18n.MessagesApi;

public class FooController extends Controller {
    private final MessagesApi messagesApi;

    @Inject
    public FooController(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public Result action() {
        return Results.ok("Hello").withoutLang(messagesApi);
    }
}
```

## `Http.Context.session()` deprecated

That means other methods that depend directly on it were also deprecated:

1. `play.mvc.Controller.session()`
1. `play.mvc.Controller.session(String key, String value)`
1. `play.mvc.Controller.session(String key)`

The new way to retrieve the session of a request is to call the `session()` method of an `Http.Request` instance. The new way to manipulate the session is to call corresponding [`play.mvc.Result`](api/java/play/mvc/Result.html) methods. For example:

### Before

```java
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

public class FooController extends Controller {
    public Result info() {
        String user = session("current_user");
        return Results.ok("Hello " + user);
    }

    public Result login() {
        session("current_user", "user@gmail.com");
        return Results.ok("Hello");
    }

    public Result logout() {
        session().remove("current_user");
        return Results.ok("Hello");
    }

    public Result clear() {
        session().clear();
        return Results.ok("Hello");
    }
}
```

### After

```java
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

import java.util.Optional;

public class FooController extends Controller {
    public Result info(Http.Request request) {
        // Get the current user or then fallback to guest
        String user = request.session().getOptional("current_user").orElse("guest");
        return Results.ok("Hello " + user);
    }

    public Result login(Http.Request request) {
        return Results.ok("Hello")
            .addingToSession(request, "current_user", "user@gmail.com");
    }

    public Result logout(Http.Request request) {
        return Results.ok("Hello")
            .removingFromSession(request, "current_user");
    }

    public Result clear() {
        return Results.ok("Hello")
            .withNewSession();
    }
}
```

## `Http.Context.flash()` deprecated

That means other methods that depend directly on it were also deprecated:

1. `play.mvc.Controller.flash()`
1. `play.mvc.Controller.flash(String key, String value)`
1. `play.mvc.Controller.flash(String key)`

The new way to retrieve the flash of a request is to call the `flash()` method of a `Http.Request` instance. The new way to manipulate the flash is to call corresponding [`play.mvc.Result`](api/java/play/mvc/Result.html) methods. For example:

### Before

```java
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

public class FooController extends Controller {
    public Result info() {
        String message = flash("message");
        return Results.ok("Message: " + message);
    }

    public Result login() {
        flash("message", "Login successful");
        return Results.redirect("/dashboard");
    }

    public Result logout() {
        flash().remove("message");
        return Results.redirect("/");
    }

    public Result clear() {
        flash().clear();
        return Results.redirect("/");
    }
}
```

### After

```java
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

public class FooController extends Controller {
    public Result info(Http.Request request) {
        String message = request.flash().getOptional("message").orElse("The default message");
        return Results.ok("Message: " + message);
    }

    public Result login() {
        return Results.redirect("/dashboard")
            .flashing("message", "Login successful");
    }

    public Result logout() {
        return Results.redirect("/")
            .removingFromFlash("message");
    }

    public Result clear() {
        return Results.redirect("/")
            .withNewFlash();
    }
}
```

## Template helper methods deprecated

Inside templates, Play offered you various helper methods which rely on `Http.Context` internally. These methods are deprecated starting with Play 2.7. Instead, you have to explicitly pass the desired object to your templates now.

### Before

```html
@()
@ctx()
@request()
@response()
@flash()
@session()
@lang()
@messages()
@Messages("some_msg_key")
```

### After

```html
@(Http.Request request, Lang lang, Messages messages)
@request
@request.flash()
@request.session()
@lang
@messages
@messages("some_msg_key")
```

There is no direct replacement for `ctx()` and `response()`.

## Some template tags need an implicit `Request`, `Messages` or `Lang` instance

Some template tags need to access a `Http.Request`, `Messages` or `Lang` instance in order to work correctly. Until now these tags just made use of `Http.Context.current()` to retrieve such instances.

Because `Http.Context` is deprecated however, such instances should now be passed as `implicit` parameters to templates which make use of such tags. By marking the parameter as `implicit` you don't always have to pass it on to the tag which actually needs it, but the tag can retrieve it from the implicit scope automatically.

> **Note:** To better understand how implicit parameters works, see [implicit parameter](https://docs.scala-lang.org/tour/implicit-parameters.html) and [where does Scala look for implicits](https://docs.scala-lang.org/tutorials/FAQ/finding-implicits.html) sections of Scala FAQ.

Following tags need an implicit `Request` instance to be present:

```html
@(arg1, arg2,...)(implicit request: Http.Request)

These tags will automatically use the implicit request passed to this template:
@helper.jsloader
@helper.script
@helper.style
@helper.javascriptRouter
@CSRF
@CSRF.formField
@CSRF.getToken
@defaultpages.devError
@defaultpages.devNotFound
@defaultpages.error
@defaultpages.badRequest
@defaultpages.notFound
@defaultpages.todo
@defaultpages.unauthorized
```

So, if you have a view that use some of the tags above, for example if you have a file `app/views/names.scala.html` like below:

```html
@(names: List[String])(implicit request: Http.Request)

<html>
    <head>
        <!-- `scripts` tags requires a request to generate a CSP nonce -->
        @script(args = 'type -> "text/javascript") {
            alert("Just a single inline script");
        }
    </head>
    <body>
        ...
    </body>
</html>
```

Your controller will need to pass the request as a parameter to the `render` method:

```java
import java.util.List;
import java.util.ArrayList;

import javax.inject.Inject;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Controller;

public class SomeController extends Controller {

    public Result action(Http.Request request) {
        List<String> names = new ArrayList<>("Jane", "James", "Rich");
        return ok(views.html.names.render(names, request));
    }
}
```

There are also the helper tags that need an implicit `Messages` instance to be present:

```html
@(arg1, arg2,...)(implicit messages: play.i18n.Messages)

These tags will automatically use the implicit messages passed to this template:
@helper.inputText
@helper.inputDate
@helper.inputCheckboxGroup
@helper.inputFile
@helper.inputRadioGroup
@helper.inputPassword
@helper.textarea
@helper.input
@helper.select
@helper.checkbox
```

So, if you have a view that use some of the tags above, for example if you have a file `app/views/userForm.scala.html` like below:

```html
@(userForm: Form[User)(implicit messages: play.i18n.Messages)

<html>
    <head>
        <title>User form page</title>
    </head>

    <body>
        @helper.form(action = routes.UsersController.save) {
            @helper.inputText(addressData("name"))
            @helper.inputText(addressData("email"))
            ...
        })
    </body>
</html>
```

Your controller will then be like:

```java
import javax.inject.Inject;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Controller;

import play.i18n.Messages;
import play.i18n.MessagesApi;

import play.data.FormFactory;

public class SomeController extends Controller {

    private final FormFactory formFactory;
    private final MessagesApi messagesApi;

    @Inject
    public SomeController(FormFactory formFactory, MessagesApi messagesApi) {
        this.formFactory = formFactory;
        this.messagesApi = messagesApi;
    }

    public Result action(Http.Request request) {
        Form<User> userForm = formFactory.form(User.class);
        // Messages instance that will be passed to render the view and
        // inside the view will be passed implicitly to helper tags.
        Messages messages = messagesApi.preferred(request);
        return ok(views.html.userForm.render(userForm, messages));
    }
}
```

> **Note:** some of these features were previously provided by `PlayMagicForJava` and were heavily depending on `Http.Context.current()`. That is why you will see warnings like:
>
> ```
> method implicitXXX in object PlayMagicForJava is deprecated (since 2.7.0): See https://www.playframework.com/documentation/latest/JavaHttpContextMigration27
> ```
>
> Passing the parameters to your view will make it more clear what is happening and where your view is depending on other data.

Play itself does not provide tags that need a `Lang` instance to be present, third-party modules however may do:

```html
@(arg1, arg2,...)(implicit lang: play.i18n.Lang)
```

Third-party tags will automatically use the implicit messages passed to this template. You can pass an implicit instance of `Lang` to your view just like the examples for `Http.Request` and `Messages` above.

## Changes in Java Forms related to `Http.Context`

When retrieving the [`Field`](api/java/play/data/Form.Field.html) of a [`Form`](api/java/play/data/Form.html) (e.g. via `myform.field("username")` or just `myform("username")` inside templates) the language of the current `Http.Context` was used to format the value of the field. Starting with Play 2.7 however this isn't the case anymore. Instead you can now explicitly set the language the form should use when retrieving a field. To make things simple and to not force you to set the language for every form explicitly, Play sets it during binding already:

```java
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Controller;

import play.data.Form;
import play.data.FormFactory;

public class MyController extends Controller {

    private final FormFactory formFactory;

    @Inject
    public MyController(FormFactory formFactory) {
        this.formFactory = formFactory;
    }

    public Result action(Http.Request request) {
        // In this example, the language of the form will be set
        // to the preferred language of the request.
        Form<User> form = formFactory.form(User.class).bindFromRequest(request);
        return ok(views.form.render(form));
    }
}
```

You can also change the language of an existing form if you need:

```java
import play.mvc.Result;
import play.mvc.Controller;

import play.i18n.Lang;
import play.data.Form;
import play.data.FormFactory;

public class MyController extends Controller {

    private final FormFactory formFactory;

    @Inject
    public MyController(FormFactory formFactory) {
        this.formFactory = formFactory;
    }

    public Result action(Http.Request request) {
        // There is first the lang from request
        Form<User> form = formFactory.form(User.class).bindFromRequest(request);

        // Let's change the language to `es`.
        Lang lang = Lang.forCode("es");
        Form<User> formWithNewLang = form.withLang(lang);
        return ok(views.form.render(formWithNewLang));
    }
}
```

> **Note:** changing the language of the the current `Http.Context` (e.g. via `Http.Context.current().changeLang(...)` or `Http.Context.current().setTransientLang(...)`) does not have an effect on the language used to retrieve the field value of a form anymore - as explained, use `form.withLang(...)` instead.

## `Http.Context` Request tags removed from `args`

Request tags, which [[have been deprecated|Migration26#Request-tags-deprecation]] in Play 2.6, have finally been removed in Play 2.7. Therefore the `args` map of an `Http.Context` instance no longer contains these removed request tags as well. Instead you can use the `request.attrs()` method now, which provides you the same request attributes.

## CSRF tokens removed from `args`

The `@AddCSRFToken` action annotation added two entries named `CSRF_TOKEN` and `CSRF_TOKEN_NAME` to the `args` map of an `Http.Context` instance. These entries have been removed. Use [[the new correct way to get the token|JavaCsrf#Getting-the-current-token]].

## RoutingDSL changes

Until Play 2.6, when using Java [[routing DSL|JavaRoutingDsl]], there is no other way to access the current `request` besides `Http.Context.current()`. Now the DSL has new methods where a request will be passed to the block.

### Before

```java
import play.mvc.Http;
import play.routing.Router;
import play.routing.RoutingDsl;

import javax.inject.Inject;

import static play.mvc.Results.ok;

public class MyRouter {

    private final RoutingDsl routingDsl;

    @Inject
    public MyRouter(RoutingDsl routingDsl) {
        this.routingDsl = routingDsl;
    }

    public Router router() {
        return routingDsl
                .GET("/hello/:to")
                .routeTo(to -> {
                    Http.Request request = Http.Context.current().request();
                    return ok("Hello " + to + ". Here is the request path: " + request.path());
                })
                .build();
    }
}
```

In the example above, we need to use `Http.Context.current()` to access the request. From now on, you can instead write the code like below:

### After

```java
import play.routing.Router;
import play.routing.RoutingDsl;

import javax.inject.Inject;

import static play.mvc.Results.ok;

public class MyRouter {

    private final RoutingDsl routingDsl;

    @Inject
    public MyRouter(RoutingDsl routingDsl) {
        this.routingDsl = routingDsl;
    }

    public Router router() {
        return routingDsl
                .GET("/hello/:to")
                .routingTo((request, to) ->
                    ok("Hello " + to + ". Here is the request path: " + request.path())
                )
                .build();
    }
}
```

An important aspect to note is that, in the new API, `Http.Request` will always be the first parameter for the function blocks.

## Disabling the `Http.Context` and JPA thread local

If you followed the above migration notes and changed all your code so it doesn't make use of API's that rely on `Http.Context` (meaning you don't get compiler warnings anymore) you can disable the `Http.Context` thread local.

Just add the following line to your `application.conf` file:

```hocon
play.allowHttpContext = false
```

To also disable the [`play.db.jpa.JPAEntityManagerContext`](api/java/play/db/jpa/JPAEntityManagerContext.html) thread local add:

```hocon
play.jpa.allowJPAEntityManagerContext = false
```
