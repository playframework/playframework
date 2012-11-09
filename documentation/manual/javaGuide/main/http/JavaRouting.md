# HTTP routing

## The built-in HTTP router

The router is the component that translates each incoming HTTP request to an action call (a static, public method in a controller class).

An HTTP request is seen as an event by the MVC framework. This event contains two major pieces of information:

- the request path (such as `/clients/1542`, `/photos/list`), including the query string.
- the HTTP method (GET, POST, ...).

Routes are defined in the `conf/routes` file, which is compiled. This means that you’ll see route errors directly in your browser:

[[images/routesError.png]]

## The routes file syntax

`conf/routes` is the configuration file used by the router. This file lists all of the routes needed by the application. Each route consists of an HTTP method and URI pattern associated with a call to an action method.

Let’s see what a route definition looks like:

```
GET   /clients/:id          controllers.Clients.show(id: Long)  
```

> Note that in the action call, the parameter type comes after the parameter name, like in Scala.

Each route starts with the HTTP method, followed by the URI pattern. The last element of a route is the call definition.

You can also add comments to the route file, with the `#` character:

```
# Display a client.
GET   /clients/:id          controllers.Clients.show(id: Long)  
```

## The HTTP method

The HTTP method can be any of the valid methods supported by HTTP (`GET`, `POST`, `PUT`, `DELETE`, `HEAD`).

## The URI pattern

The URI pattern defines the route’s request path. Some parts of the request path can be dynamic.

### Static path

For example, to exactly match `GET /clients/all` incoming requests, you can define this route:

```
GET   /clients              controllers.Clients.list()
```

### Dynamic parts 

If you want to define a route that, say, retrieves a client by id, you need to add a dynamic part:

```
GET   /clients/:id          controllers.Clients.show(id: Long)  
```

> Note that a URI pattern may have more than one dynamic part.

The default matching strategy for a dynamic part is defined by the regular expression `[^/]+`, meaning that any dynamic part defined as `:id` will match exactly one URI path segment.

### Dynamic parts spanning several /

If you want a dynamic part to capture more than one URI path segment, separated by forward slashes, you can define a dynamic part using the `*id` syntax, which uses the `.*` regular expression:

```
GET   /files/*name          controllers.Application.download(name)  
```

Here, for a request like `GET /files/images/logo.png`, the `name` dynamic part will capture the `images/logo.png` value.

### Dynamic parts with custom regular expressions

You can also define your own regular expression for a dynamic part, using the `$id<regex>` syntax:
    
```
GET   /clients/$id<[0-9]+>  controllers.Clients.show(id: Long)  
```

## Call to action generator method

The last part of a route definition is the call. This part must define a valid call to an action method.

If the method does not define any parameters, just give the fully-qualified method name:

```
GET   /                     controllers.Application.homePage()
```

If the action method defines parameters, the corresponding parameter values will be searched for in the request URI, either extracted from the URI path itself, or from the query string.

```
# Extract the page parameter from the path.
# i.e. http://myserver.com/index
GET   /:page                controllers.Application.show(page)
```

Or:

```
# Extract the page parameter from the query string.
# i.e. http://myserver.com/?page=index
GET   /                     controllers.Application.show(page)
```

Here is the corresponding `show` method definition in the `controllers.Application` controller:

```java
public static Result show(String page) {
  String content = Page.getContentOf(page);
  response().setContentType("text/html");
  return ok(content);
}
```

### Parameter types

For parameters of type `String`, the parameter type is optional. If you want Play to transform the incoming parameter into a specific Scala type, you can add an explicit type:

```
GET   /client/:id           controllers.Clients.show(id: Long)
```

Then use the same type for the corresponding action method parameter in the controller:

```java
public static Result show(Long id) {
  Client client = Client.findById(id);
  return ok(views.html.Client.show(client));
}
```

> **Note:** The parameter types are specified using a suffix syntax. Also The generic types are specified using the `[]` symbols instead of `<>`, as in Java. For example, `List[String]` is the same type as the Java `List<String>`.

### Parameters with fixed values

Sometimes you’ll want to use a fixed value for a parameter:

```
# Extract the page parameter from the path, or fix the value for /
GET   /                     controllers.Application.show(page = "home")
GET   /:page                controllers.Application.show(page)
```

### Parameters with default values

You can also provide a default value that will be used if no value is found in the incoming request:

```
# Pagination links, like /clients?page=3
GET   /clients              controllers.Clients.list(page: Integer ?= 1)
```

## Routing priority

Many routes can match the same request. If there is a conflict, the first route (in declaration order) is used.

## Reverse routing

The router can be used to generate a URL from within a Java call. This makes it possible to centralize all your URI patterns in a single configuration file, so you can be more confident when refactoring your application.

For each controller used in the routes file, the router will generate a ‘reverse controller’ in the `routes` package, having the same action methods, with the same signature, but returning a `play.mvc.Call` instead of a `play.mvc.Result`. 

The `play.mvc.Call` defines an HTTP call, and provides both the HTTP method and the URI.

For example, if you create a controller like:

```java
package controllers;

import play.*;
import play.mvc.*;

public class Application extends Controller {
    
  public static Result hello(String name) {
      return ok("Hello " + name + "!");
  }
    
}
```

And if you map it in the `conf/routes` file:

```
# Hello action
GET   /hello/:name          controllers.Application.hello(name)
```

You can then reverse the URL to the `hello` action method, by using the `controllers.routes.Application` reverse controller:

```java
// Redirect to /hello/Bob
public static Result index() {
    return redirect(controllers.routes.Application.hello("Bob")); 
}
```


##Imports
If you are tired to always write `controller.Application`, then you can define  default import

```scala
val main = PlayProject(…).settings(
  routesImport += "controller._"
)
```

> **Next:** [[Manipulating the response | JavaResponse]]