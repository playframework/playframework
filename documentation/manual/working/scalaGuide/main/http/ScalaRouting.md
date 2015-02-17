<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# HTTP routing

## The built-in HTTP router

The router is the component in charge of translating each incoming HTTP request to an Action.

An HTTP request is seen as an event by the MVC framework. This event contains two major pieces of information:

- the request path (e.g. `/clients/1542`, `/photos/list`), including the query string
- the HTTP method (e.g. GET, POST, …).

Routes are defined in the `conf/routes` file, which is compiled. This means that you’ll see route errors directly in your browser:

[[images/routesError.png]]

## Dependency Injection

Play supports generating two types of routers, one is a dependency injected router, the other is a static router.  The default is the static router, but if you created a new Play application using the Play seed Activator templates, your project will include the following configuration in `build.sbt` telling it to use the injected router:

```scala
routesGenerator := InjectedRoutesGenerator
```

The code samples in Play's documentation assumes that you are using the injected routes generator.  If you are not using this, you can trivially adapt the code samples for the static routes generator, either by prefixing the controller invocation part of the route with an `@` symbol, or by declaring each of your controllers as an `object` rather than a `class`.

## The routes file syntax

`conf/routes` is the configuration file used by the router. This file lists all of the routes needed by the application. Each route consists of an HTTP method and URI pattern, both associated with a call to an `Action` generator.

Let’s see what a route definition looks like:

@[clients-show](code/scalaguide.http.routing.routes)

Each route starts with the HTTP method, followed by the URI pattern. The last element is the call definition.

You can also add comments to the route file, with the `#` character.

@[clients-show-comment](code/scalaguide.http.routing.routes)

## The HTTP method

The HTTP method can be any of the valid methods supported by HTTP (`GET`, `POST`, `PUT`, `DELETE`, `HEAD`).

## The URI pattern

The URI pattern defines the route’s request path. Parts of the request path can be dynamic.

### Static path

For example, to exactly match incoming `GET /clients/all` requests, you can define this route:

@[static-path](code/scalaguide.http.routing.routes)

### Dynamic parts 

If you want to define a route that retrieves a client by ID, you’ll need to add a dynamic part:

@[clients-show](code/scalaguide.http.routing.routes)

> Note that a URI pattern may have more than one dynamic part.

The default matching strategy for a dynamic part is defined by the regular expression `[^/]+`, meaning that any dynamic part defined as `:id` will match exactly one URI part.

### Dynamic parts spanning several /

If you want a dynamic part to capture more than one URI path segment, separated by forward slashes, you can define a dynamic part using the `*id` syntax, which uses the `.+` regular expression:

@[spanning-path](code/scalaguide.http.routing.routes)

Here for a request like `GET /files/images/logo.png`, the `name` dynamic part will capture the `images/logo.png` value.

### Dynamic parts with custom regular expressions

You can also define your own regular expression for the dynamic part, using the `$id<regex>` syntax:
    
@[regex-path](code/scalaguide.http.routing.routes)

## Call to the Action generator method

The last part of a route definition is the call. This part must define a valid call to a method returning a `play.api.mvc.Action` value, which will typically be a controller action method.

If the method does not define any parameters, just give the fully-qualified method name:

@[home-page](code/scalaguide.http.routing.routes)

If the action method defines some parameters, all these parameter values will be searched for in the request URI, either extracted from the URI path itself, or from the query string.

@[page](code/scalaguide.http.routing.routes)

Or:

@[page](code/scalaguide.http.routing.query.routes)

Here is the corresponding, `show` method definition in the `controllers.Application` controller:

@[show-page-action](code/ScalaRouting.scala)

### Parameter types

For parameters of type `String`, typing the parameter is optional. If you want Play to transform the incoming parameter into a specific Scala type, you can explicitly type the parameter:

@[clients-show](code/scalaguide.http.routing.routes)

And do the same on the corresponding `show` method definition in the `controllers.Clients` controller:

@[show-client-action](code/ScalaRouting.scala)

### Parameters with fixed values

Sometimes you’ll want to use a fixed value for a parameter:

@[page](code/scalaguide.http.routing.fixed.routes)

### Parameters with default values

You can also provide a default value that will be used if no value is found in the incoming request:

@[clients](code/scalaguide.http.routing.defaultvalue.routes)

### Optional parameters

You can also specify an optional parameter that does not need to be present in all requests:

@[optional](code/scalaguide.http.routing.routes)

## Routing priority

Many routes can match the same request. If there is a conflict, the first route (in declaration order) is used.

## Reverse routing

The router can also be used to generate a URL from within a Scala call. This makes it possible to centralize all your URI patterns in a single configuration file, so you can be more confident when refactoring your application.

For each controller used in the routes file, the router will generate a ‘reverse controller’ in the `routes` package, having the same action methods, with the same signature, but returning a `play.api.mvc.Call` instead of a `play.api.mvc.Action`. 

The `play.api.mvc.Call` defines an HTTP call, and provides both the HTTP method and the URI.

For example, if you create a controller like:

@[reverse-controller](code/ScalaRouting.scala)

And if you map it in the `conf/routes` file:

@[route](code/scalaguide.http.routing.reverse.routes)

You can then reverse the URL to the `hello` action method, by using the `controllers.routes.Application` reverse controller:

@[reverse-router](code/ScalaRouting.scala)
