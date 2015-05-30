<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Removing `GlobalSettings`

If you are keen to use dependency injection, we are recommending that you move out of your `GlobalSettings` implementation class as much code as possible. Ideally, you should be able to refactor your code so that it is possible to eliminate your `GlobalSettings` class altogether.

Next follows a method-by-method guide for refactoring your code. Because the APIs are slightly different for Java and Scala, make sure to jump to the appropriate subsection.

> Note: If you haven't yet read about dependency injection in Play, make a point to do it now. Follow the appropriate link to learn about dependency injection in Play with [[Java|JavaDependencyInjection]] or [[Scala|ScalaDependencyInjection]].

## Scala

* `GlobalSettings.beforeStart` and `GlobalSettings.onStart`:  Anything that needs to happen on start up should now be happening in the constructor of a dependency injected class. A class will perform its initialisation when the dependency injection framework loads it. If you need eager initialisation (because you need to execute some code *before* the application is actually started), [[define an eager binding|ScalaDependencyInjection#Eager-bindings]].

* `GlobalSettings.onStop`: Add a dependency to [`ApplicationLifecycle`](api/scala/play/api/inject/ApplicationLifecycle.html) on the class that needs to register a stop hook. Then, move the implementation of your `GlobalSettings.onStop` method inside the `Future` passed to the `ApplicationLifecycle.addStopHook`. Read [[Stopping/cleaning-up|ScalaDependencyInjection#Stopping/cleaning-up]] for more information.

* `GlobalSettings.onError`: Create a class that inherits from [`HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html), and move the implementation of your `GlobalSettings.onError` inside the `HttpErrorHandler.onServerError` method. Read [[Error Handling|ScalaErrorHandling]] for more information.

* `GlobalSettings.onRequestReceived`:  Create a class that inherits from [`HttpRequestHandler`](api/scala/play/api/http/HttpRequestHandler.html), and move the implementation of your `GlobalSettings.onRequestReceived` inside the `HttpRequestHandler.handlerForRequest` method.  Read [[Request Handlers|ScalaHttpRequestHandlers]] for more information.
Be aware that if in your `GlobalSettings.onRequestReceived` implementation you are calling `super.onRequestReceived`, then you should inherits from [`DefaultHttpRequestHandler`](api/scala/play/api/http/DefaultHttpRequestHandler.html) instead of `HttpRequestHandler`, and replace all calls to `super.onRequestReceived` with `super.handlerForRequest`.

* `GlobalSettings.onRouteRequest`: Create a class that inherits from [`DefaultHttpRequestHandler`](api/scala/play/api/http/DefaultHttpRequestHandler.html), and move the implementation of your `GlobalSettings.onRouteRequest` method inside the `DefaultHttpRequestHandler.routeRequest` method. Read [[Request Handlers|ScalaHttpRequestHandlers]] for more information.

* `GlobalSettings.onRequestCompletion`: This method is deprecated, and it is *no longer invoked by Play*. Instead, create a custom filter that attaches an `onDoneEnumerating` callback onto the returned `Enumerator` result. Read [[Scala Http Filters|ScalaHttpFilters]] for details on how to create a http filter.

* `GlobalSettings.onHandlerNotFound`: Create a class that inherits from [`HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html), and provide an implementation for `HttpErrorHandler.onClientError`. Read [[Error Handling|ScalaErrorHandling]] for more information.
Note that `HttpErrorHandler.onClientError` takes a `statusCode` in argument, hence your implementation should boil down to:

```scala
if(statusCode == play.api.http.Status.NOT_FOUND) {
  // move your implementation of `GlobalSettings.onHandlerNotFound` here
}
```

* `GlobalSettings.onBadRequest`: Create a class that inherits from [`HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html), and provide an implementation for `HttpErrorHandler.onClientError`. Read [[Error Handling|ScalaErrorHandling]] for more information.
Note that `HttpErrorHandler.onClientError` takes a `statusCode` in argument, hence your implementation should boil down to:

```scala
if(statusCode == play.api.http.Status.BAD_REQUEST) {
  // move your implementation of `GlobalSettings.onBadRequest` here
}
```

* `GlobalSettings.configure` and `GlobalSettings.onLoadConfig`: Specify all configuration in your config file or create your own ApplicationLoader (see [[GuiceApplicationBuilder.loadConfig|ScalaDependencyInjection#Advanced:-Extending-the-GuiceApplicationLoader]]).

* `GlobalSettings.doFilter`: Create a class that inherits from [`HttpFilters`](api/scala/play/api/http/HttpFilters.html), and provide an implementation for `HttpFilter.filters`. Read [[Http Filters|ScalaHttpFilters]] for more information.

Also, mind that if your `Global` class is mixing the `WithFilters` trait, you should now create a Filter class that inherits from [`HttpFilters`](api/scala/play/api/http/HttpFilters.html), and place it in the empty package. Read [[here|ScalaHttpFilters]] for more details.


## Java

* `GlobalSettings.beforeStart` and `GlobalSettings.onStart`:  Anything that needs to happen on start up should now be happening in the constructor of a dependency injected class. A class will perform its initialisation when the dependency injection framework loads it. If you need eager initialisation (for example, because you need to execute some code *before* the application is actually started), [[define an eager binding|JavaDependencyInjection#Eager-bindings]].

* `GlobalSettings.onStop`: Add a dependency to [`ApplicationLifecycle`](api/java/play/inject/ApplicationLifecycle.html) on the class that needs to register a stop hook. Then, move the implementation of your `GlobalSettings.onStop` method inside the `Promise` passed to the `ApplicationLifecycle.addStopHook`. Read [[Stopping/cleaning-up|JavaDependencyInjection#Stopping/cleaning-up]] for more information.

* `GlobalSettings.onError`: Create a class that inherits from [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html), and move the implementation of your `GlobalSettings.onError` inside the `HttpErrorHandler.onServerError` method. Read [[Error Handling|JavaErrorHandling]] for more information.

* `GlobalSettings.onRequest`: Create a class that inherits from [`DefaultHttpRequestHandler`](api/java/play/http/DefaultHttpRequestHandler.html), and move the implementation of your `GlobalSettings.onRequest` method inside the `DefaultHttpRequestHandler.createAction` method. Read [[Request Handlers|JavaHttpRequestHandlers]] for more information.

* `GlobalSettings.onRouteRequest`: There is no simple migration for this method when using the Java API. If you need this, you will have to keep your Global class around for a little longer.

* `GlobalSettings.onHandlerNotFound`: Create a class that inherits from [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html), and provide an implementation for `HttpErrorHandler.onClientError`. Read [[Error Handling|JavaErrorHandling]] for more information.
Note that `HttpErrorHandler.onClientError` takes a `statusCode` in argument, hence your implementation should boil down to:

```java
if(statusCode == play.mvc.Http.Status.NOT_FOUND) {
  // move your implementation of `GlobalSettings.onHandlerNotFound` here
}
```

* `GlobalSettings.onBadRequest`: Create a class that inherits from [`HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html), and provide an implementation for `HttpErrorHandler.onClientError`. Read [[Error Handling|JavaErrorHandling]] for more information.
Note that `HttpErrorHandler.onClientError` takes a `statusCode` in argument, hence your implementation should boil down to:

```java
if(statusCode == play.mvc.Http.Status.BAD_REQUEST) {
  // move your implementation of `GlobalSettings.onBadRequest` here
}
```

* `GlobalSettings.onLoadConfig`: Specify all configuration in your config file or create your own ApplicationLoader (see [[GuiceApplicationBuilder.loadConfig|JavaDependencyInjection#Advanced:-Extending-the-GuiceApplicationLoader]]).

* `GlobalSettings.filters`: Create a class that inherits from [`HttpFilters`](api/java/play/http/HttpFilters.html), and provide an implementation for `HttpFilter.filters`. Read [[Http Filters|JavaHttpFilters]] for more information.