<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# HTTP routing

## The built-in HTTP router

The router is the component that translates each incoming HTTP request to an action call (a public method in a controller class).

An HTTP request is seen as an event by the MVC framework. This event contains two major pieces of information:

- the request path (such as `/clients/1542`, `/photos/list`), including the query string.
- the HTTP method (GET, POST, ...).

Routes are defined in the `conf/routes` file, which is compiled. This means that you’ll see route errors directly in your browser:

[[images/routesError.png]]

## Dependency Injection

Play supports generating two types of routers, one is a dependency injected router, the other is a static router.  The default is the static router, but if you created a new Play application using the Play seed Activator templates, your project will include the following configuration in `build.sbt` telling it to use the injected router:

```scala
routesGenerator := InjectedRoutesGenerator
```

The code samples in Play's documentation assumes that you are using the injected routes generator.  If you are not using this, you can trivially adapt the code samples for the static routes generator, either by prefixing the controller invocation part of the route with an `@` symbol, or by declaring each of your action methods as `static`.

## The routes file syntax

`conf/routes` is the configuration file used by the router. This file lists all of the routes needed by the application. Each route consists of an HTTP method and URI pattern associated with a call to an action method.

Let’s see what a route definition looks like:

@[clients-show](code/javaguide.http.routing.routes)

> Note that in the action call, the parameter type comes after the parameter name, like in Scala.

Each route starts with the HTTP method, followed by the URI pattern. The last element of a route is the call definition.

You can also add comments to the route file, with the `#` character:

@[clients-show-comment](code/javaguide.http.routing.routes)

## The HTTP method

The HTTP method can be any of the valid methods supported by HTTP (`GET`, `PATCH`, `POST`, `PUT`, `DELETE`, `HEAD`).

## The URI pattern

The URI pattern defines the route’s request path. Some parts of the request path can be dynamic.

### Static path

For example, to exactly match `GET /clients/all` incoming requests, you can define this route:

@[static-path](code/javaguide.http.routing.routes)

### Dynamic parts 

If you want to define a route that, say, retrieves a client by id, you need to add a dynamic part:

@[clients-show](code/javaguide.http.routing.routes)

> Note that a URI pattern may have more than one dynamic part.

The default matching strategy for a dynamic part is defined by the regular expression `[^/]+`, meaning that any dynamic part defined as `:id` will match exactly one URI path segment.

### Dynamic parts spanning several /

If you want a dynamic part to capture more than one URI path segment, separated by forward slashes, you can define a dynamic part using the `*id` syntax, which uses the `.*` regular expression:

@[spanning-path](code/javaguide.http.routing.routes)

Here, for a request like `GET /files/images/logo.png`, the `name` dynamic part will capture the `images/logo.png` value.

### Dynamic parts with custom regular expressions

You can also define your own regular expression for a dynamic part, using the `$id<regex>` syntax:
    
@[regex-path](code/javaguide.http.routing.routes)

## Call to action generator method

The last part of a route definition is the call. This part must define a valid call to an action method.

If the method does not define any parameters, just give the fully-qualified method name:

@[home-page](code/javaguide.http.routing.routes)

If the action method defines parameters, the corresponding parameter values will be searched for in the request URI, either extracted from the URI path itself, or from the query string.

@[page](code/javaguide.http.routing.routes)

Or:

@[page](code/javaguide.http.routing.query.routes)

Here is the corresponding `show` method definition in the `controllers.Application` controller:

@[show-page-action](code/javaguide/http/routing/controllers/Application.java)

### Parameter types

For parameters of type `String`, the parameter type is optional. If you want Play to transform the incoming parameter into a specific Scala type, you can add an explicit type:

@[clients-show](code/javaguide.http.routing.routes)

Then use the same type for the corresponding action method parameter in the controller:

@[clients-show-action](code/javaguide/http/routing/controllers/Clients.java)

> **Note:** The parameter types are specified using a suffix syntax. Also, the generic types are specified using the `[]` symbols instead of `<>`, as in Java. For example, `List[String]` is the same type as the Java `List<String>`.

### Parameters with fixed values

Sometimes you’ll want to use a fixed value for a parameter:

@[page](code/javaguide.http.routing.fixed.routes)

### Parameters with default values

You can also provide a default value that will be used if no value is found in the incoming request:

@[clients](code/javaguide.http.routing.defaultvalue.routes)

### Optional parameters

You can also specify an optional parameter that does not need to be present in all requests:

@[optional](code/javaguide.http.routing.routes)

## Routing priority

Many routes can match the same request. If there is a conflict, the first route (in declaration order) is used.

## Reverse routing

The router can be used to generate a URL from within a Java call. This makes it possible to centralize all your URI patterns in a single configuration file, so you can be more confident when refactoring your application.

For each controller used in the routes file, the router will generate a ‘reverse controller’ in the `routes` package, having the same action methods, with the same signature, but returning a `play.mvc.Call` instead of a `play.mvc.Result`. 

The `play.mvc.Call` defines an HTTP call, and provides both the HTTP method and the URI.

For example, if you create a controller like:

@[controller](code/javaguide/http/routing/reverse/controllers/Application.java)

And if you map it in the `conf/routes` file:

@[hello](code/javaguide.http.routing.reverse.routes)

You can then reverse the URL to the `hello` action method, by using the `controllers.routes.Application` reverse controller:

@[reverse-redirect](code/javaguide/http/routing/controllers/Application.java)

> **Note:** There is a `routes` subpackage for each controller package. So the action `controllers.admin.Application.hello` can be reversed via `controllers.admin.routes.Application.hello`.
