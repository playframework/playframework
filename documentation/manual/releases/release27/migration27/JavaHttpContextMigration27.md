<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->

# Java `Http.Context` changes

Multiple changes were made to `Http.Context`. The idea is to move more and more away from `Http.Context` which has needs a Thread Local and it is hard to test and to misuse.

### `Http.Context` Request tags removed from `args` 

Request tags, which [[have been deprecated|Migration26#Request-tags-deprecation]] in Play 2.6, have finally been removed in Play 2.7.
Therefore the `args` map of a `Http.Context` instance no longer contains these removed request tags as well.
Instead you can use the `contextObj.request().attrs()` method now, which provides you the equivalent request attributes.

### `Http.Response` deprecated

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
    public CompletionStage<Result> call(Http.Context ctx) {
        return delegate.call(ctx)
                .thenApply(result -> result.withHeader("Name", "Value"));
    }
}
```

### `Http.Context.changeLang` and `Http.Context.clearLang` deprecated

That means other methods that depends directly on these two were also deprecated:

1. `play.mvc.Controller.changeLang`
1. `play.mvc.Controller.clearLang`

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

If you are using `changeLang` to change the `Lang` used to render a template, you should now pass the `Lang` as a parameter. This will make the template clearer and easier to read. For example:

```html
@(implicit lang: Lang)
@{messages().at("hello")}
```

> **Note**: declaring it as `implicit` will make it available to `messages()` helpers without the need to adapt all these calls.

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