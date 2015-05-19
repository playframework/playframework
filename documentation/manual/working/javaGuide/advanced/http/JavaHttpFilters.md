<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Filters

Play provides a simple filter API for applying global filters to each request.

## Filters vs action composition

The filter API is intended for cross cutting concerns that are applied indiscriminately to all routes.  For example, here are some common use cases for filters:

* Logging/metrics collection
* [[GZIP encoding|GzipEncoding]]
* [[Security headers|SecurityHeaders]]

In contrast, [[action composition|JavaActionsComposition]] is intended for route specific concerns, such as authentication and authorisation, caching and so on.  If your filter is not one that you want applied to every route, consider using action composition instead, it is far more powerful.  And don't forget that you can create your own action builders that compose your own custom defined sets of actions to each route, to minimise boilerplate.

## Using filters

The simplest way to use a filter is to provide an implementation of the [`HttpFilters`](api/java/play/http/HttpFilters.html) interface in the root package called `Filters`:

@[filters](code/javaguide/httpfilters/Filters.java)

If you want to have different filters in different environments, or would prefer not putting this class in the root package, you can configure where Play should find the class by setting `play.http.filters` in `application.conf` to the fully qualified class name of the class.  For example:

    play.http.filters=com.example.Filters

## Where do filters fit in?

Filters wrap the action after the action has been looked up by the router.  This means you cannot use a filter to transform a path, method or query parameter to impact the router.  However you can direct the request to a different action by invoking that action directly from the filter, though be aware that this will bypass the rest of the filter chain.  If you do need to modify the request before the router is invoked, a better way to do this would be to place your logic in `Global.onRouteRequest` instead.

Since filters are applied after routing is done, it is possible to access routing information from the request, via the `tags` map on the `RequestHeader`.  For example, you might want to log the time against the action method.  In that case, you might update the `logTime` method to look like this:

> Routing tags are a feature of the Play router.  If you use a custom router, or return a custom action in `Global.onRouteRequest`, these parameters may not be available.

## Creating a filter

Creating a filter using Java is currently not ideal because you will need to work with Scala types such as `Iteratee`, which have been originally designed to be consumed from Scala code, and can be quite cumbersome to use in Java. Our reccomendation is to create your custom filters using Scala, and then use them from Java as explained in [[this section|JavaHttpFilters#Using-filters]].

Read [[here|ScalaHttpFilters]] to learn how to create a custom filter in Scala.