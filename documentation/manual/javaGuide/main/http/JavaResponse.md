# Manipulating the response

## Changing the default Content-Type

The result content type is automatically inferred from the Java value you specify as body.

For example:

```
Result textResult = ok("Hello World!");
```

Will automatically set the `Content-Type` header to `text/plain`, while:

```
Result jsonResult = ok(jerksonObject);
```

will set the `Content-Type` header to `application/json`.

This is pretty useful, but sometimes you want to change it. Just use the `as(newContentType)` method on a result to create a new similiar result with a different `Content-Type` header:

```
Result htmlResult = ok("<h1>Hello World!</h1>").as("text/html");
```

You can also set the content type on the HTTP response:

```
public static Result index() {
  response().setContentType("text/html");
  return ok("<h1>Hello World!</h1>");
}
```

## Setting HTTP response headers

You can add (or update) any HTTP response header:

```
public static Result index() {
  response().setContentType("text/html");
  response().setHeader(CACHE_CONTROL, "max-age=3600");
  response().setHeader(ETAG, "xxx");
  return ok("<h1>Hello World!</h1>");
}
```

Note that setting an HTTP header will automatically discard any previous value.

## Setting and discarding cookies

Cookies are just a special form of HTTP headers, but Play provides a set of helpers to make it easier.

You can easily add a Cookie to the HTTP response:

```
response().setCookie("theme", "blue");
```

Also, to discard a Cookie previously stored on the Web browser:

```
response().discardCookies("theme");
```

## Specifying the character encoding for text results

For a text-based HTTP response it is very important to handle the character encoding correctly. Play handles that for you and uses `utf-8` by default.

The encoding is used to both convert the text response to the corresponding bytes to send over the network socket, and to add the proper `;charset=xxx` extension to the `Content-Type` header.

The encoding can be specified when you are generating the `Result` value:

```
public static Result index() {
  response().setContentType("text/html; charset=iso-8859-1");
  return ok("<h1>Hello World!</h1>", "iso-8859-1");
}
```

> **Next:** [[Session and Flash scopes | JavaSessionFlash]]