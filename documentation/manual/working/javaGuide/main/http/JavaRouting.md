<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# HTTP routing

## The built-in HTTP router

The router is the component that translates each incoming HTTP request to an action call (a public method in a controller class).

An HTTP request is seen as an event by the MVC framework. This event contains two major pieces of information:

- the request path (such as `/clients/1542`, `/photos/list`), including the query string.
- the HTTP method (GET, POST, ...).

Routes are defined in the `conf/routes` file, which is compiled. This means that you’ll see route errors directly in your browser:

[[images/routesError.png]]

## Dependency Injection

Play's default routes generator creates a router class that accepts controller instances in an `@Inject`-annotated constructor. That means the class is suitable for use with dependency injection and can also be instantiated manually using the constructor.

Play also comes with a legacy static routes generator that works with controllers that declare actions as static methods. This is generally not recommended because it breaks encapsulation, makes code less testable, and is incompatible with many of Play's new APIs.

If you need to use static controllers, you can switch to the static routes generator by adding the following configuration to your `build.sbt`.

```scala
routesGenerator := StaticRoutesGenerator
```

The code samples in Play's documentation assume that you are using the injected routes generator. If you are not using this, you can trivially adapt the code samples for the static routes generator, either by prefixing the controller invocation part of the route with an `@` symbol, or by declaring each of your action methods as `static`.

## The routes file syntax

`conf/routes` is the configuration file used by the router. This file lists all of the routes needed by the application. Each route consists of an HTTP method and URI pattern associated with a call to an action method.

Let’s see what a route definition looks like:

@[clients-show](code/javaguide.http.routing.routes)

> **Note:** in the action call, the parameter type comes after the parameter name, like in Scala.

Each route starts with the HTTP method, followed by the URI pattern. The last element of a route is the call definition.

You can also add comments to the route file, with the `#` character:

@[clients-show-comment](code/javaguide.http.routing.routes)

## The HTTP method

The HTTP method can be any of the valid methods supported by HTTP (`GET`, `PATCH`, `POST`, `PUT`, `DELETE`, `HEAD`, `OPTIONS`).

## The URI pattern

The URI pattern defines the route’s request path. Some parts of the request path can be dynamic.

### Static path

For example, to exactly match `GET /clients/all` incoming requests, you can define this route:

@[static-path](code/javaguide.http.routing.routes)

### Dynamic parts

If you want to define a route that, say, retrieves a client by id, you need to add a dynamic part:

@[clients-show](code/javaguide.http.routing.routes)

> **Note:** A URI pattern may have more than one dynamic part.

The default matching strategy for a dynamic part is defined by the regular expression `[^/]+`, meaning that any dynamic part defined as `:id` will match exactly one URI path segment. Unlike other pattern types, path segments are automatically URI-decoded in the route, before being passed to your controller, and encoded in the reverse route.

### Dynamic parts spanning several /

If you want a dynamic part to capture more than one URI path segment, separated by forward slashes, you can define a dynamic part using the `*id` syntax, also known as a wildcard pattern, which uses the `.*` regular expression:

@[spanning-path](code/javaguide.http.routing.routes)

Here, for a request like `GET /files/images/logo.png`, the `name` dynamic part will capture the `images/logo.png` value.

Note that *dynamic parts spanning several `/` are not decoded by the router or encoded by the reverse router*. It is your responsibility to validate the raw URI segment as you would for any user input. The reverse router simply does a string concatenation, so you will need to make sure the resulting path is valid, and does not, for example, contain multiple leading slashes or non-ASCII characters.

### Dynamic parts with custom regular expressions

You can also define your own regular expression for a dynamic part, using the `$id<regex>` syntax:

@[regex-path](code/javaguide.http.routing.routes)


Just like with wildcard routes, the parameter is *not decoded by the router or encoded by the reverse router*. You're responsible for validating the input to make sure it makes sense in that context.

It is also possible to apply modifiers by preceding the route with a line starting with a `+`. This can change the behavior of certain Play components. One such modifier is the "nocsrf" modifier to bypass the [[CSRF filter|JavaCsrf]]:

@[nocsrf](code/javaguide.http.routing.routes)

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

> **Note:** There is a `routes` subpackage for each controller package. So the action `controllers.Application.hello` can be reversed via `controllers.routes.Application.hello` (as long as there is no other route before it in the routes file that happens to match the generated path).

The reverse action method works quite simply: it takes your parameters and substitutes them back into the route pattern.  In the case of path segments (`:foo`), the value is encoded before the substitution is done.  For regex and wildcard patterns the string is substituted in raw form, since the value may span multiple segments.  Make sure you escape those components as desired when passing them to the reverse route, and avoid passing unvalidated user input.

## Relative routes

There are instances where returning a relative route instead of an absolute may be useful.  The routes returned by `play.mvc.Call` are always absolute (they lead with a `/`), which can lead to problems when requests to your web application are rewritten by HTTP proxies, load balancers, and API gateways.  Some examples where using a relative route would be useful include:

* Hosting an app behind a web gateway that prefixes all routes with something other than what is configured in your `conf/routes` file, and roots your application at a route it's not expecting.
* When dynamically rendering stylesheets, you need asset links to be relative because they may end up getting served from different URLs by a CDN.

To be able to generate a relative route you need to know what to make the target route relative to (the start route).  The start route can be retrieved from the current `RequestHeader`.  Therefore, to generate a relative route it's required that you pass in your current `RequestHeader` or the start route as a `String` parameter.

For example, given controller endpoints like:

@[relative-controller](code/javaguide/http/routing/relative/controllers/Relative.java)

> **Note:** The current request is passed to the view template by calling `request()`

And if you map it in the `conf/routes` file:

@[relative-hello](code/javaguide.http.routing.relative.routes)

You can then define relative routes using the reverse router as before and include an additional call to `relativeTo(play.mvc.RequestHeader requestHeader)`:

@[relative-hello-view](code/javaguide/http/routing/relative/views/hello.scala.html)

> **Note:** The `Http.Request` passed from the controller is cast to a `Http.RequestHeader` in the view parameters.

When requesting `/foo/bar/hello` the generated HTML will look like so:

@[relative-hello-html](code/javaguide/http/routing/relative/views/hello.html)

## The Default Controller

Play includes a [`Default` controller](api/scala/controllers/Default.html) which provides a handful of useful actions. These can be invoked directly from the routes file:

@[defaultcontroller](code/javaguide.http.routing.defaultcontroller.routes)

In this example, `GET /` redirects to an external website, but it's also possible to redirect to another action (such as `/posts` in the above example).

## Advanced Routing

See [[Routing DSL|JavaRoutingDsl]].
