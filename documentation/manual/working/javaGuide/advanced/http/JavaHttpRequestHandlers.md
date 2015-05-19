<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# HTTP Request Handlers

Play abstraction for handling requests is less sophisticated in the Play Java API, when compared to its Scala counterpart. However, it may still be enough if you only need to execute some code before the controller's action method associated to the passed request is executed. If that's not enough, then you should fall back to the [[Scala API|ScalaHttpRequestHandlers]] for creating a HTTP request handler.

## Implementing a custom request handler

The [`HttpRequestHandler`](api/java/play/http/HttpRequestHandler.html) interface has two methods that needs to be implemented: 

* `createAction`: Takes the request and the controller's action method associated with the passed request.
*  `wrapAction`: Takes the action to be run and allows for a final global interceptor to be added to the action.

There is also a [`DefaultHttpRequestHandler`](api/java/play/http/DefaultHttpRequestHandler.html) class that can be used if you don't want to implement both methods.

>> Note: If you are providing an implementation of `wrapAction` because you need to apply a cross cutting concern to an action before is executed, creating a [[filter|JavaHttpFilters]] is a more idiomatic way of achieving the same.

## Configuring the http request handler

A custom http handler can be supplied by creating a class in the root package called `RequestHandler` that implements `HttpRequestHandler`, for example:

@[default](code/javaguide/RequestHandler.java)

If you don’t want to place your request handler in the root package, or if you want to be able to configure different request handlers for different environments, you can do this by configuring the `play.http.requestHandler` configuration property in `application.conf`:

    play.http.requestHandler = "com.example.RequestHandler"
    
### Performance notes

The http request handler that Play uses if none is configured is one that delegates to the legacy `GlobalSettings` methods.  This may have a performance impact as it will mean your application has to do many lookups out of Guice to handle a single request.  If you are not using a `Global` object, then you don't need this, instead you can configure Play to use the default http request handler:

    play.http.requestHandler = "play.http.DefaultHttpRequestHandler"
