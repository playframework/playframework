<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Actions, Controllers and Results

## What is an Action?

Most of the requests received by a Play application are handled by an `Action`. 

A `play.api.mvc.Action` is basically a `(play.api.mvc.Request => play.api.mvc.Result)` function that handles a request and generates a result to be sent to the client.

@[echo-action](code/ScalaActions.scala)

An action returns a `play.api.mvc.Result` value, representing the HTTP response to send to the web client. In this example `Ok` constructs a **200 OK** response containing a **text/plain** response body.

## Building an Action

The `play.api.mvc.Action` companion object offers several helper methods to construct an Action value. 

The first simplest one just takes as argument an expression block returning a `Result`:

@[zero-arg-action](code/ScalaActions.scala)

This is the simplest way to create an Action, but we don't get a reference to the incoming request. It is often useful to access the HTTP request calling this Action. 

So there is another Action builder that takes as an argument a function `Request => Result`:

@[request-action](code/ScalaActions.scala)

It is often useful to mark the `request` parameter as `implicit` so it can be implicitly used by other APIs that need it:

@[implicit-request-action](code/ScalaActions.scala)

The last way of creating an Action value is to specify an additional `BodyParser` argument:

@[json-parser-action](code/ScalaActions.scala)

Body parsers will be covered later in this manual.  For now you just need to know that the other methods of creating Action values use a default **Any content body parser**.

## Controllers are action generators

A `Controller` is nothing more than a singleton object that generates `Action` values. 

The simplest use case for defining an action generator is a method with no parameters that returns an `Action` value	:

@[full-controller](code/ScalaActions.scala)

Of course, the action generator method can have parameters, and these parameters can be captured by the `Action` closure:

@[parameter-action](code/ScalaActions.scala)

## Simple results

For now we are just interested in simple results: An HTTP result with a status code, a set of HTTP headers and a body to be sent to the web client.

These results are defined by `play.api.mvc.Result`:

@[simple-result-action](code/ScalaActions.scala)

Of course there are several helpers available to create common results such as the `Ok` result in the sample above:

@[ok-result-action](code/ScalaActions.scala)

This produces exactly the same result as before.

Here are several examples to create various results:

@[other-results](code/ScalaActions.scala)

All of these helpers can be found in the `play.api.mvc.Results` trait and companion object.

## Redirects are simple results too

Redirecting the browser to a new URL is just another kind of simple result. However, these result types don't take a response body.

There are several helpers available to create redirect results:

@[redirect-action](code/ScalaActions.scala)

The default is to use a `303 SEE_OTHER` response type, but you can also set a more specific status code if you need one:

@[moved-permanently-action](code/ScalaActions.scala)

## "TODO" dummy page

You can use an empty `Action` implementation defined as `TODO`: the result is a standard ‘Not implemented yet’ result page:

@[todo-action](code/ScalaActions.scala)
