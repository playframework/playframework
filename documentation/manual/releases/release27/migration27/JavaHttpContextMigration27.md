<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->

# Java `Http.Context` changes

Multiple changes were made to `Http.Context`. The idea is to move more and more away from `Http.Context` which uses a thread local. The methods on Result are easier to test and harder to misuse.

### `Http.Context` Request tags removed from `args` 

Request tags, which [[have been deprecated|Migration26#Request-tags-deprecation]] in Play 2.6, have finally been removed in Play 2.7.
Therefore the `args` map of a `Http.Context` instance no longer contains these removed request tags as well.
Instead you can use the `request.attrs()` method now, which provides you the equivalent request attributes.

### CSRF tokens removed from `args`

The `@AddCSRFToken` action annotation added two entries named `CSRF_TOKEN` and `CSRF_TOKEN_NAME` to the `args` map of a `Http.Context` instance. These entries have been removed.
Use [[the new correct way to get the token|JavaCsrf#Getting-the-current-token]].

### `Http.Context.current()` and `Http.Context.request()` deprecated

That means other methods that depend directly on these two were also deprecated:

1. `play.mvc.Controller.ctx()`
1. `play.mvc.Controller.request()`

Before Play 2.7, when using Play with Java, the only way to access the `Http.Request` was `Http.Context.current()` which was used internally by `Controller.request()` method. The problem with `Http.Context.current()` is that it is implemented using a thread local, which is harder to test, to keep in sync with changes made by other places and makes it harder to access the request in other threads.

With Play 2.7 you can now access the current request by simply adding it as a param to your routes and actions.

For example the routes files contains:

```
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

If you did use `Http.Context.current()` in other places besides controllers you have to pass the desired data via method parameters to these places now.
Look at this example which checks if the current request's remote address is on a blacklist:

#### Before

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

#### After

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

### `Action.call(Context)` deprecated

If you are using [[action composition|JavaActionsComposition]] you have to update your actions to avoid `Http.Context`.

#### Before

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

#### After

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

### `Http.Context.response()` and `Http.Response` class deprecated

That means other methods that depend directly on these were also deprecated:

1. `play.mvc.Controller.response()`

`Http.Response` was deprecated with other accesses methods to it. It was mainly used to add headers and cookies, but these are already available in `play.mvc.Result` and then the API got a little confused. For Play 2.7, you should migrate code like:

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

Should be written as:

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

If you have action composition that depends on `Http.Context.response`, you can also rewrite it like. For example, the code below:

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

Should be written as:

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

### `lang()`, `changeLang(...)`, `clearLang()`, `setTransientLang(...)`, `clearTransientLang()` and `messages()` of `Http.Context` deprecated

That means other methods that depend directly on these were also deprecated:

1. `play.mvc.Controller.lang()`
1. `play.mvc.Controller.changeLang(Lang lang)`
1. `play.mvc.Controller.changeLang(String code)`
1. `play.mvc.Controller.clearLang()`

The new way of changing lang now is to have a instance of [`play.i18n.MessagesApi`](api/java/play/i18n/MessagesApi.html) injected and call corresponding [`play.mvc.Result`](api/java/play/mvc/Result.html) methods. For example:

#### Before

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

#### After

```java
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

import play.i18n.MessagesApi;

public class FooController extends Controller {
    private final MessagesApi messagesApi;

    public FooController(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public Result action() {
        return Results.ok("Hello").withLang(Lang.forCode("es"), messagesApi);
    }
}
```

If you are using `changeLang` to change the `Lang` used to render a template, you should now pass the `Messages` itself as a parameter. This will make the template clearer and easier to read. For example in an action method you have to create a `Messages` instance like:

```java
Messages messages = this.messagesApi.preferred(Lang.forCode("es"));
return ok(myview.render(messages));
```
Or if you want to have a fallback to the languages of the request you can do that as well:
```java
Lang lang = Lang.forCode("es");
// Get a Message instance based on the spanish locale, however if that isn't available
// try to choose the best fitting language based on the current request
Messages messages = this.messagesApi.preferred(request.withTransientLang(lang));
return ok(myview.render(messages));
```
> **Note**: To not repeat that code again and again inside each action method you could e.g. create the `Messages` instance in an action of the [[action composition chain|JavaActionsComposition]] and save that instance in a request Attribute so you can access it later.

Now the template:

```html
@(implicit messages: play.i18n.Messages)
@{messages.at("hello")}
```

> **Note**: Declaring `messages` as `implicit` will make it available to sub-views that implicitly require a `MessagesProvider` without the need to pass them one.

And the same applies to `clearLang`:

#### Before

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

#### After

```java
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

import play.i18n.MessagesApi;

public class FooController extends Controller {
    private final MessagesApi messagesApi;

    public FooController(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    public Result action() {
        return Results.ok("Hello").clearingLang(messagesApi);
    }
}
```

### `Http.Context.session()` deprecated

That means other methods that depend directly on it were also deprecated:

1. `play.mvc.Controller.session()`
1. `play.mvc.Controller.session(String key, String value)`
1. `play.mvc.Controller.session(String key)`

The new way to retrieve the session of a request is the call the `session()` method of a `Http.Request` instance.
The new way to manipulate the session is to call corresponding [`play.mvc.Result`](api/java/play/mvc/Result.html) methods. For example:

#### Before

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

#### After

```java
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

public class FooController extends Controller {
    public Result info(Http.Request request) {
        String user = request.session().get("current_user");
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

### `Http.Context.flash()` deprecated

That means other methods that depend directly on it were also deprecated:

1. `play.mvc.Controller.flash()`
1. `play.mvc.Controller.flash(String key, String value)`
1. `play.mvc.Controller.flash(String key)`

The new way to retrieve the flash of a request is the call the `flash()` method of a `Http.Request` instance.
The new way to manipulate the flash is to call corresponding [`play.mvc.Result`](api/java/play/mvc/Result.html) methods. For example:

#### Before

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

#### After

```java
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.Controller;

public class FooController extends Controller {
    public Result info(Http.Request request) {
        String message = request.flash().get("message");
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

### Changes in Java Forms related to `Http.Context`

When retrieving the [`Field`](api/java/play/data/Form.Field.html) of a [`Form`](api/java/play/data/Form.html) (e.g. via `myform.field("username")` or just `myform("username")` inside templates) the language of the current `Http.Context` was used to format the value of the field.
Starting with Play 2.7 however this isn't the case anymore.
Instead you can now explicitly set the language the form should use when retrieving a field:

```java
Form<User> formWithNewLang = currentForm.withLang(lang);
```

To make things simple and to not force you to explicitly set the language for every form Play does set it during binding already:

```java
Form<User> form = formFactory().form(User.class).bindFromRequest(request);
```

In this example the language of the form will be set to the preferred language of the request.

Be aware that changing the language of the the current `Http.Context` (e.g. via `Http.Context.current().changeLang(...)` or `Http.Context.current().setTransientLang(...)`) does not have an effect on the language used to retrieve the field value of a form anymore - as explained, use `form.withLang(...)` instead.
