<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.1?

## Migration to Scala 2.10 

The whole runtime API for Play have been migrated to Scala 2.10 allowing your applications to take part of the new great features provided by this new language release. 

In the same time we have broken the dependency that was existing between the Scala version used by the _Build system_ (sbt), and the Scala version used at runtime. This way, it makes it easier to build and test Play on experimental or unstable branches of the Scala language.

## Migration to `scala.concurrent.Future`

One of the greatest feature provided by Scala 2.10 is the new standard `scala.concurrent.Future` library for managing asynchronous code in Scala. Play is now based itself on this API, and its great asynchronous HTTP and streaming features are now compatible directly with any other library using the same API. 

It makes it even simpler to use Play with Akka, or any upcoming asynchronous datastore driver that will use this new API.

In the same time, we worked to simplify the execution context model, and by providing an easy way to choose for each part of your application, the underlying `ExecutionContext` used to run your code. 

## Modularization

The Play project has been split on many sub-projects, allowing you to select the minimal set of dependency needed for your project. 

You have to select the exact set of optional dependencies your application need, in the list of:

- `jdbc` : The **JDBC** connection pool and the `play.api.db` API. 
- `anorm` : The **Anorm** component.
- `javaCore` : The core **Java** API.
- `javaJdbc` : The Java database API.
- `javaEbean` : The Ebean plugin for Java.
- `javaJpa` : The JPA plugin for Java.
- `filters` : A set of build-in filters for Play (such as the CSRF filter)

The `play` core project has now a very limited set of external dependencies and can be used as a minimal asynchronous, high performance HTTP server without the other components.

## Allow more modularization for your projects

To allow, to compose and reuse components in your own projects further, Play 2.1 supports sub-Router composition.

For example, a sub project can define its own router component, using its own namespace, such as:

In `conf/my.subproject.routes`

```
GET   /                   my.subproject.controllers.Application.index
```

And then, you can integrate this component into your main application, by wiring the Router, such as:

```
# The home page
GET   /                   controllers.Application.index

# Include a sub-project
->    /my-subproject      my.subproject.Routes

# The static assets
GET   /public/*file       controllers.Assets.at(file)
```

In the configuration, at runtime a call to the `/my-subproject` URL will eventually invoke the `my.subproject.controllers.Application.index` Action.

> Note: in order to avoid name collision issues with the main application, always make sure that you define a subpackage within your controller classes that belong to a sub project (i.e. `my.subproject` in this particular example). You'll also need to make sure that the subproject's Assets controller is defined in the same name space.

More information about this feature can be found at [[Working with sub-projects|SBTSubProjects]].


## `Http.Context` propagation in the Java API

In Play 2.0, the HTTP context was lost during the asynchronous callback, since these code fragment are run on different thread than the original one handling the HTTP request.

Consider:

```
public static Result index() {
  return async(
    aServiceSomewhere.getData().map(new Function<String,Result>(data) {
      // Ouch! You try to access the request data in an asynchronous callback
      String user = session().get("user"); 
      return ok("Here is the result " + user + ": " + data);
    })
  );
}
```

This code wasn't working this way. For really good reason, if you think about the underlying asynchronous architecture, but yet it was really surprising for Java developers.

We eventually found a way to solve this problem and to propagate the `Http.Context` over a stack spanning several threads, so this code is now working this way.

## Better threading model for the Java API

While running asynchronous code over mutable data structures, chances are right that you hit some race conditions if you don't synchronize properly your code. Because Play promotes highly asynchronous and non-blocking code, and because Java data structure are mostly mutable and not thread-safe, it is the responsibility of your code to deal with the synchronization issues.

Consider:

```
public static Result index() {

  final HashMap<String,Integer> result = new HashMap<String,Integer>();

  aService.doSomethingAsync().map(new Function<String,String>(key) {
    Integer i = result.get(key);
    if(i != null) {
      result.set(key, i++);
    }
    return key;
  });

  aService.doSomethingElse().map(new Function<String,String>(key) {
    result.remove(key);
    return null;
  });

  ...
}
```

In this code, chances are really high to hit a race condition if the 2 callbacks are run in the same time on 2 different threads, while accessing each to the share `result` HashMap. And the consequence will be probably some pseudo-deadlock in your application because of the implementation of the underlying Java HashMap.

To avoid any of these problems, we decided to manage the callback execution synchronization at the framework level. Play 2.1 will never run concurrently 2 callbacks for the same `Http.Context`. In this context, all callback execution will be run sequentially (while there is no guarantees that they will run on the same thread).

## Managed Controller classes instantiation

By default Play binds URLs to controller methods statically, that is, no Controller instances are created by the framework and the appropriate static method is invoked depending on the given URL. In certain situations, however, you may want to manage controller creation and that’s when the new routing syntax comes handy.

Route definitions starting with @ will be managed by the `Global::getControllerInstance` method, so given the following route definition:

```
GET     /                  @controllers.Application.index()
```

Play will invoke the `getControllerInstance` method which in return will provide an instance of `controllers.Application` (by default this is happening via the default constructor). Therefore, if you want to manage controller class instantiation either via a dependency injection framework or manually you can do so by overriding getControllerInstance in your application’s Global class.

As this example [demonstrates it](https://github.com/guillaumebort/play20-spring-demo), it allows to wire any dependency injection framework such as __Spring__ into your Play application.

## New Scala JSON API

The new Scala JSON API provide great features such as transformation and validation of JSON tree. Check the new documentation at the [[Scala Json combinators document|ScalaJsonCombinators]].

## New Filter API and CSRF protection

Play 2.1 provides a new and really powerful filter API allowing to intercept each part of the HTTP request or response, in a fully non-blocking way.

For that, we introduced a new new simpler type replacing the old `Action[A]` type, called `EssentialAction` which is defined as:

```
RequestHeader => Iteratee[Array[Byte], Result]
```

As a result, a filter is simply defined as: 

```
EssentialAction => EssentialAction
```

> __Note__: The old `Action[A]` type is still available for compatibility.

The `filters` project that is part of the standard Play distribution contain a set of standard filter, such as the `CSRF` providing automatic token management against the CSRF security issue. 

## RequireJS

In play 2.0 the default behavior for Javascript was to use google closure's commonJS module support. In 2.1 this was changed to use [requireJS](http://requirejs.org/) instead.

What this means in practice is that by default Play will only minify and combine files in stage, dist, start modes only. In dev mode Play will resolve dependencies client side.

If you wish to use this feature, you will need to add your modules to the settings block of your build file:

```
requireJs := "main.js"
```

More information about this feature can be found on the [[RequireJS documentation page|RequireJS-support]].

## Content negotiation

The support of content negotiation has been improved, for example now controllers automatically choose the most appropriate lang according to the quality values set in the `Accept-Language` request header value.

It is also easier to write Web Services supporting several representations of a same resource and automatically choosing the best according to the `Accept` request header value:

```
val list = Action { implicit request =>
  val items = Item.findAll
  render {
    case Accepts.Html() => Ok(views.html.list(items))
    case Accepts.Json() => Ok(Json.toJson(items))
  }
}
```

More information can be found on the content negotiation documentation pages for [[Scala|ScalaContentNegotiation]] and [[Java|JavaContentNegotiation]].
