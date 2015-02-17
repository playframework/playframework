<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# HTTP Request Handlers

Play provides a range of abstractions for routing requests to actions, providing routers and filters to allow most common needs.  Sometimes however an application will have more advanced needs that aren't met by Play's abstractions.  When this is the case, applications can provide custom implementations of Play's lowest level HTTP pipeline API, the [`HttpRequestHandler`](api/scala/index.html#play.api.http.HttpRequestHandler).

Providing a custom `HttpRequestHandler` should be a last course of action.  Most custom needs can be met through implementing a custom router or a [[filter|ScalaHttpFilters]].

## Implementing a custom request handler

The `HttpRequestHandler` trait has one method to be implemented, `handlerForRequest`.  This takes the request to get a handler for, and returns a tuple of a `RequestHeader` and a `Handler`.

The reason why a request header is returned is so that information can be added to the request, for example, routing information.  In this way, the router is able to tag requests with routing information, such as which route matched the request, which can be useful for monitoring or even for injecting cross cutting functionality.

A very simple request handler that simply delegates to a router might look like this:

@[simple](code/ScalaHttpRequestHandlers.scala)

## Extending the default request handler

In most cases you probably won't want to create a new request handler from scratch, you'll want to build on the default one.  This can be done by extending [DefaultHttpRequestHandler](api/scala/index.html#play.api.http.HttpRequestHandler).  The default request handler provides a number of methods that can be overridden, this allows you to implement your custom functionality without reimplementing the code to tag requests, handle errors, etc.

One use case for a custom request handler may be that you want to delegate to a different router, depending on what host the request is for.  Here is an example of how this might be done:

@[virtualhost](code/ScalaHttpRequestHandlers.scala)

## Configuring the http request handler

To tell Play to use your custom http request handler, simply configure `play.http.requestHandler` in `application.conf` to point to the fully qualified class name of your handler:

    play.http.requestHandler = "com.example.MyHttpRequestHandler"
    
### Performance notes

The http request handler that Play uses if none is configured is one that delegates to the legacy `GlobalSettings` methods.  This may have a performance impact as it will mean your application has to do many lookups out of Guice to handle a single request.  If you are not using a `Global` object, then you don't need this, instead you can configure Play to use the default http request handler:

    play.http.requestHandler = "play.api.http.DefaultHttpRequestHandler"
