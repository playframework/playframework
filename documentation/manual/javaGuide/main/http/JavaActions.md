# Actions, Controllers and Results

## What is an Action?

Most of the requests received by a Play application are handled by an `Action`. 

An action is basically a Java method that processes the request parameters, and produces a result to be sent to the client.

```
public static Result index() {
  return ok("Got request " + request() + "!");
}
```

An action returns a `play.mvc.Result` value, representing the HTTP response to send to the web client. In this example `ok` constructs a **200 OK** response containing a **text/plain** response body.

## Controllers 

A controller is nothing more than a class extending `play.mvc.Controller` that groups several action methods.

The simplest syntax for defining an action is a static method with no parameters that returns a `Result` value:

```
public static Result index() {
  return ok("It works!");
}
```

An action method can also have parameters:

```
public static Result index(String name) {
  return ok("Hello" + name);
}
```

These parameters will be resolved by the `Router` and will be filled with values from the request URL. The parameter values can be extracted from either the URL path or the URL query string.

## Results

Let’s start with simple results: an HTTP result with a status code, a set of HTTP headers and a body to be sent to the web client.

These results are defined by `play.mvc.Result`, and the `play.mvc.Results` class provides several helpers to produce standard HTTP results, such as the `ok` method we used in the previous section:

```
public static Result index() {
  return ok("Hello world!");
}
```

Here are several examples that create various results:

```
Result ok = ok("Hello world!");
Result notFound = notFound();
Result pageNotFound = notFound("<h1>Page not found</h1>").as("text/html");
Result badRequest = badRequest(views.html.form.render(formWithErrors));
Result oops = internalServerError("Oops");
Result anyStatus = status(488, "Strange response type");
```

All of these helpers can be found in the `play.mvc.Results` class.

## Redirects are simple results too

Redirecting the browser to a new URL is just another kind of simple result. However, these result types don't have a response body.

There are several helpers available to create redirect results:

```
public static Result index() {
  return redirect("/user/home");
}
```

The default is to use a `303 SEE_OTHER` response type, but you can also specify a more specific status code:

```
public static Result index() {
  return temporaryRedirect("/user/home");
}
```

> **Next:** [[HTTP Routing | JavaRouting]]


