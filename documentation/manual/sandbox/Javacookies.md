# Using cookies

## Request’s cookies

You can retrieve the request’s cookies using the `cookies()` method of a `Http.Request` object:

```java
public class Application extends Controller {

    public static Result index() {
        Http.Cookie cookie = request().cookies().get("foo");
        if (cookie.value().equals("bar")) {
          // ...
        }
    }
}
```

The `get(String name)` method returns the cookie or `null` if there was no such cookie. See the [API documentation](http://playframework.github.com/api/java/play/mvc/Http.html) of the `Http.Cookie` class to know what information is available on cookies.

## Response’s cookies

Setting cookies and discarding them is performed through the `setCookie` and `discardCookies` methods of the `Http.Response` object:

```java
public class Application extends Controller {

    public static Result index() {

        // Set cookies
        response().setCookie("bar", "baz");
        response().setCookie("bar", "baz", 3600);

        // Discard cookies
        response().discardCookies("foo", "bar");

        // ...
    }
}
```

See the various overloaded versions of the `setCookie` method in the [API documentation](http://playframework.github.com/api/java/play/mvc/Http.Response.html) to know all the possible combinations of parameters.