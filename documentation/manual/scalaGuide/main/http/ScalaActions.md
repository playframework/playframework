# Actions, Controllers and Results

## What is an Action?

Most of the requests received by a Play application are handled by an `Action`. 

A `play.api.mvc.Action` is basically a `(play.api.mvc.Request => play.api.mvc.Result)` function that handles a request and generates a result to be sent to the client.

```scala
val echo = Action { request =>
  Ok("Got request [" + request + "]")
}
```

An action returns a `play.api.mvc.Result` value, representing the HTTP response to send to the web client. In this example `Ok` constructs a **200 OK** response containing a **text/plain** response body.

## Building an Action

The `play.api.mvc.Action` companion object offers several helper methods to construct an Action value. 

The first simplest one just takes as argument an expression block returning a `Result`:

```scala
Action {
  Ok("Hello world")
}
```

This is the simplest way to create an Action, but we don't get a reference to the incoming request. It is often useful to access the HTTP request calling this Action. 

So there is another Action builder that takes as an argument a function `Request => Result`:

```scala
Action { request =>
  Ok("Got request [" + request + "]")
}
```

It is often useful to mark the `request` parameter as `implicit` so it can be implicitly used by other APIs that need it:

```scala
Action { implicit request =>
  Ok("Got request [" + request + "]")
}
```

The last way of creating an Action value is to specify an additional `BodyParser` argument:

```scala
Action(parse.json) { implicit request =>
  Ok("Got request [" + request + "]")
}
```

Body parsers will be covered later in this manual.  For now you just need to know that the other methods of creating Action values use a default **Any content body parser**.

## Controllers are action generators

A `Controller` is nothing more than a singleton object that generates `Action` values. 

The simplest use case for defining an action generator is a method with no parameters that returns an `Action` value	:

```scala
package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok("It works!")
  }
    
}
```

Of course, the action generator method can have parameters, and these parameters can be captured by the `Action` closure:

```scala
def hello(name: String) = Action {
  Ok("Hello " + name)
}
```

## Simple results

For now we are just interested in simple results: An HTTP result with a status code, a set of HTTP headers and a body to be sent to the web client.

These results are defined by `play.api.mvc.SimpleResult`:

```scala
def index = Action {
  SimpleResult(
    header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/plain")), 
    body = Enumerator("Hello world!")
  )
}
```

Of course there are several helpers available to create common results such as the `Ok` result in the sample above:

```scala
def index = Action {
  Ok("Hello world!")
}
```

This produces exactly the same result as before.

Here are several examples to create various results:

```scala
val ok = Ok("Hello world!")
val notFound = NotFound
val pageNotFound = NotFound(<h1>Page not found</h1>)
val badRequest = BadRequest(views.html.form(formWithErrors))
val oops = InternalServerError("Oops")
val anyStatus = Status(488)("Strange response type")
```

All of these helpers can be found in the `play.api.mvc.Results` trait and companion object.

## Redirects are simple results too

Redirecting the browser to a new URL is just another kind of simple result. However, these result types don't take a response body.

There are several helpers available to create redirect results:

```scala
def index = Action {
  Redirect("/user/home")
}
```

The default is to use a `303 SEE_OTHER` response type, but you can also set a more specific status code if you need one:

```scala
def index = Action {
  Redirect("/user/home", status = MOVED_PERMANENTLY)
}
```

## "TODO" dummy page

You can use an empty `Action` implementation defined as `TODO`: the result is a standard ‘Not implemented yet’ result page:

```scala
def index(name:String) = TODO
```

> **Next:** [[HTTP Routing | ScalaRouting]]
