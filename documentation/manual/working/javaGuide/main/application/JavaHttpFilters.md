<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Filters

Play provides a simple filter API for applying global filters to each request.

## Filters vs action composition

The filter API is intended for cross cutting concerns that are applied indiscriminately to all routes.  For example, here are some common use cases for filters:

* Logging/metrics collection
* [[GZIP encoding|GzipEncoding]]
* [[Security headers|SecurityHeaders]]

In contrast, [[action composition|JavaActionsComposition]] is intended for route specific concerns, such as authentication and authorization, caching and so on.  If your filter is not one that you want applied to every route, consider using action composition instead, it is far more powerful.  And don't forget that you can create your own action builders that compose your own custom defined sets of actions to each route, to minimize boilerplate.

## A simple logging filter

The following is a simple filter that times and logs how long a request takes to execute in Play Framework, which implements the [`Filter`](api/java/play/mvc/Filter.html) trait:

@[simple-filter](code/javaguide/application/httpfilters/LoggingFilter.java)

Let's understand what's happening here.  The first thing to notice is the signature of the `apply` method.  The first parameter, `nextFilter`, is a function that takes a request header and produces a result. The second parameter, `requestHeader`, is the actual request header of the incoming request.

The `nextFilter` parameter represents the next action in the filter chain. Invoking it will cause the action to be invoked. In most cases you will want to invoke this at some point in your future. You may decide to not invoke it if for some reason you want to block the request.

We save a timestamp before invoking the next filter in the chain. Invoking the next filter returns a `CompletionStage<Result>` that will redeemed eventually. Take a look at the [[Handling asynchronous results|JavaAsync]] chapter for more details on asynchronous results. We then manipulate the `Result` in the future by calling the `thenApply` method with a closure that takes a `Result`. We calculate the time it took for the request, log it and send it back to the client in the response headers by calling `result.withHeader("Request-Time", "" + requestTime)`.

## Using filters

The simplest way to use a filter is to provide an implementation of the [`HttpFilters`](api/java/play/http/HttpFilters.html) interface in the root package called `Filters`. Typically you should extend the [`DefaultHttpFilters`](api/java/play/http/DefaultHttpFilters.html) class and pass your filters to the varargs constructor:

@[filters](code/javaguide/application/httpfilters/Filters.java)

If you want to have different filters in different environments, or would prefer not putting this class in the root package, you can configure where Play should find the class by setting `play.http.filters` in `application.conf` to the fully qualified class name of the class.  For example:

    play.http.filters=com.example.Filters

## Where do filters fit in?

Filters wrap the action after the action has been looked up by the router.  This means you cannot use a filter to transform a path, method or query parameter to impact the router. However you can direct the request to a different action by invoking that action directly from the filter, though be aware that this will bypass the rest of the filter chain. If you do need to modify the request before the router is invoked, a better way to do this would be to place your logic in [[ a `HttpRequestHandler`|JavaActionCreator#HTTP-request-handlers]] instead.

Since filters are applied after routing is done, it is possible to access routing information from the request, via the `attrs` map on the `RequestHeader`. For example, you might want to log the time against the action method. In that case, you might update the filter to look like this:

@[routing-info-access](code/javaguide/application/httpfilters/RoutedLoggingFilter.java)

> **Note:** Routing attributes are a feature of the Play router. If you use a custom router these parameters may not be available.

## More powerful filters

Play provides a lower level filter API called [`EssentialFilter`](api/java/play/mvc/EssentialFilter.html) that gives you full access to the body of the request. This API allows you to wrap [[EssentialAction|JavaEssentialAction]] with another action.

Here is the above filter example rewritten as an `EssentialFilter`:

@[essential-filter-example](code/javaguide/application/httpfilters/EssentialLoggingFilter.java)

The key difference here, apart from creating a new `EssentialAction` to wrap the passed in `next` action, is when we invoke next, we get back an [`Accumulator`](api/java/play/libs/streams/Accumulator.html).  You could compose this with an Akka Streams Flow using the `through` method some transformations to the stream if you wished.  We then `map` the result of the iteratee and thus handle it.

> **Note:** Although it may seem that there are two different filter APIs, there is only one, `EssentialFilter`.  The simpler `Filter` API in the earlier examples extends `EssentialFilter`, and implements it by creating a new `EssentialAction`.  The passed in callback makes it appear to skip the body parsing by creating a promise for the `Result`, while the body parsing and the rest of the action are executed asynchronously.
