<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# HTTP Request Handlers

Play provides a range of abstractions for routing requests to actions, providing routers and filters to allow most common needs.  Sometimes however an application will have more advanced needs that aren't met by Play's abstractions.  When this is the case, applications can provide custom implementations of Play's lowest level HTTP pipeline API, the [`HttpRequestHandler`](api/scala/play/api/http/HttpRequestHandler.html).

Providing a custom `HttpRequestHandler` should be a last course of action.  Most custom needs can be met through implementing a custom router or a [[filter|ScalaHttpFilters]].

## Implementing a custom request handler

The `HttpRequestHandler` trait has one method to be implemented, `handlerForRequest`.  This takes the request to get a handler for, and returns a tuple of a `RequestHeader` and a `Handler`.

The reason why a request header is returned is so that information can be added to the request, for example, routing information.  In this way, the router is able to tag requests with routing information, such as which route matched the request, which can be useful for monitoring or even for injecting cross cutting functionality.

A very simple request handler that simply delegates to a router might look like this:

@[simple](code/ScalaHttpRequestHandlers.scala)

## Extending the default request handler

In most cases you probably won't want to create a new request handler from scratch, you'll want to build on the default one.  This can be done by extending [DefaultHttpRequestHandler](api/scala/play/api/http/HttpRequestHandler.html).  The default request handler provides a number of methods that can be overridden, this allows you to implement your custom functionality without reimplementing the code to tag requests, handle errors, etc.

One use case for a custom request handler may be that you want to delegate to a different router, depending on what host the request is for.  Here is an example of how this might be done:

@[virtualhost](code/ScalaHttpRequestHandlers.scala)

## Configuring the http request handler

If you're using [`BuiltInComponents`](api/scala/play/api/BuiltInComponents.html) to construct your app, override the `httpRequestHandler` method to return an instance of your custom handler.

If you're using runtime dependency injection (e.g. Guice), the request handler can be dynamically loaded at runtime. The simplest way is to create a class in the root package called `RequestHandler` that implements `HttpRequestHandler`.

If you don’t want to place your request handler in the root package, or if you want to be able to configure different request handlers for different environments, you can do this by configuring the `play.http.requestHandler` configuration property in `application.conf`:

    play.http.requestHandler = "com.example.RequestHandler"
