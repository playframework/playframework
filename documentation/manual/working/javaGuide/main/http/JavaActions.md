<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Actions, Controllers and Results

## What is an Action?

Most of the requests received by a Play application are handled by an `Action`. 

An action is basically a Java method that processes the request parameters, and produces a result to be sent to the client.

@[simple-action](code/javaguide/http/JavaActions.java)

An action returns a `play.mvc.Result` value, representing the HTTP response to send to the web client. In this example `ok` constructs a **200 OK** response containing a **text/plain** response body.

## Controllers 

A controller is nothing more than a class extending `play.mvc.Controller` that groups several action methods.

@[full-controller](code/javaguide/http/full/Application.java)

The simplest syntax for defining an action is a method with no parameters that returns a `Result` value, as shown above.

An action method can also have parameters:

@[params-action](code/javaguide/http/JavaActions.java)

These parameters will be resolved by the `Router` and will be filled with values from the request URL. The parameter values can be extracted from either the URL path or the URL query string.

## Results

Let’s start with simple results: an HTTP result with a status code, a set of HTTP headers and a body to be sent to the web client.

These results are defined by `play.mvc.Result`, and the `play.mvc.Results` class provides several helpers to produce standard HTTP results, such as the `ok` method we used in the previous section:

@[simple-result](code/javaguide/http/JavaActions.java)

Here are several examples that create various results:

@[other-results](code/javaguide/http/JavaActions.java)

All of these helpers can be found in the `play.mvc.Results` class.

## Redirects are simple results too

Redirecting the browser to a new URL is just another kind of simple result. However, these result types don't have a response body.

There are several helpers available to create redirect results:

@[redirect-action](code/javaguide/http/JavaActions.java)

The default is to use a `303 SEE_OTHER` response type, but you can also specify a more specific status code:

@[temporary-redirect-action](code/javaguide/http/JavaActions.java)
