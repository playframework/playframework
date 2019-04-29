<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Intercepting HTTP requests

Play's Java APIs provide two ways of intercepting action calls. The first is called `ActionCreator`, which provides a `createAction` method that is used to create the initial action used in action composition. It handles calling the actual method for your action, which allows you to intercept requests.

The second way is to implement your own `HttpRequestHandler`, which is the primary entry point for all HTTP requests in Play. This includes requests from both Java and Scala actions.

## Action creators

The [`ActionCreator`](api/java/play/http/ActionCreator.html) interface has two methods that can be implemented:

* `createAction`: Takes the request and the controller's action method associated with the passed request. The action can either be the first or the last action depending on the configuration setting `play.http.actionComposition.executeActionCreatorActionFirst`.
*  `wrapAction`: Takes the action to be run and allows for a final global interceptor to be added to the action. This method is deprecated since the same can be achieved using `createAction` and the above setting.

There is also a [`DefaultActionCreator`](api/java/play/http/ActionCreator.html) interface you can extend with default implementations.

> **Note:** If you are implementing a custom ActionCreator because you need to apply a cross cutting concern to an action before it is executed, creating a [[filter|JavaHttpFilters]] is a more idiomatic way of achieving the same.

A custom action creator can be supplied by creating a class in the root package called `ActionCreator` that implements `play.http.ActionCreator`, for example:

@[default](code/javaguide/ActionCreator.java)

If you don’t want to place this class in the root package, or if you want to be able to configure different action handlers for different environments, you can do this by configuring the `play.http.actionCreator` configuration property in `application.conf`:

    play.http.actionCreator = "com.example.MyActionCreator"

> **Note:** If you are also using [[action composition|JavaActionsComposition]] then the action returned by the ```createAction``` method is executed **after** the action composition ones by default. If you want to change this order set ```play.http.actionComposition.executeActionCreatorActionFirst = true``` in ```application.conf```.

## HTTP request handlers

Sometimes an application will have more advanced needs that aren't met by Play's abstractions. When this is the case, applications can provide custom implementations of Play's lowest level HTTP pipeline API, the [`HttpRequestHandler`](api/java/play/http/HttpRequestHandler.html).

Providing a custom `HttpRequestHandler` should be a last course of action. Most custom needs can be met through implementing a custom router or a [[filter|JavaHttpFilters]].

### Implementing a custom request handler

The `HttpRequestHandler` interface has one method to be implemented, `handlerForRequest`.  This takes the request to get a handler for, and returns a `HandlerForRequest` instance containing a `RequestHeader` and a `Handler`.

The reason why a request header is returned is so that information, such as routing information, can be added to the request. In this way, the router is able to tag requests with routing information, such as which route matched the request, which can be useful for monitoring or even for injecting cross cutting functionality.

A very simple request handler that simply delegates to a router might look like this:

@[simple](code/javaguide/http/SimpleHttpRequestHandler.java)

Note that we need to inject `JavaHandlerComponents` and call `handler.withComponents` for the Java handler. This is required for Java actions to work. This will also be handled for you automatically if you extend `DefaultHttpRequestHandler` and call `super.handlerForRequest()`.

Note that `HttpRequestHandler` currently has two legacy methods with default implementations that have since been moved to `ActionCreator`.

### Configuring the http request handler

If you're using [`BuiltInComponents`](api/java/play/BuiltInComponents.html) to construct your app, override the `httpRequestHandler` method to return an instance of your custom handler.

If you're using runtime dependency injection (e.g. Guice), the request handler can be dynamically loaded at runtime. The simplest way is to create a class in the root package called `RequestHandler` that implements `HttpRequestHandler`.

If you don’t want to place your request handler in the root package, or if you want to be able to configure different request handlers for different environments, you can do this by configuring the `play.http.requestHandler` configuration property in `application.conf`:

    play.http.requestHandler = "com.example.RequestHandler"
