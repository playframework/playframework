<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Routing DSL

Play provides a DSL for routers directly in code.  This DSL has many uses, including embedding a light weight Play server, providing custom or more advanced routing capabilities to a regular Play application, and mocking REST services for testing.

The DSL uses a path pattern syntax similar to Play's compiled routes files, extracting parameters out, and invoking actions implemented using lambdas.

The DSL is provided by [`RoutingDsl`](api/java/play/routing/RoutingDsl.html).  Since you will be implementing actions, you may want to import the static methods from [`Controller`](api/java/play/mvc/Controller.html), which includes factory methods for creating results, accessing the request, response and session.  So typically you will want at least the following imports:

@[imports](code/javaguide/advanced/routing/JavaRoutingDsl.java)

And then you may use [[Dependency Injection|JavaDependencyInjection]] to get a `RoutingDsl` instance:

@[inject](code/javaguide/advanced/routing/JavaRoutingDsl.java)

Or you can directly create a new instance:

@[new-routing-dsl](code/javaguide/advanced/routing/JavaRoutingDsl.java)

A simple example of the DSL's use is:

@[simple](code/javaguide/advanced/routing/JavaRoutingDsl.java)

The `:to` parameter is extracted out and passed as the first parameter to the router.  Note that the name you give to parameters in the path pattern is irrelevant, the important thing is that parameters in the path are in the same order as parameters in your lambda.  You can have anywhere from 0 to 3 parameters in the path pattern, and other HTTP methods, such as `POST`, `PUT` and `DELETE` are supported.

Like Play's compiled router, the DSL also supports matching multi path segment parameters, this is done by prefixing the parameter with `*`:

@[full-path](code/javaguide/advanced/routing/JavaRoutingDsl.java)

Regular expressions are also supported, by prefixing the parameter with a `$` and post fixing the parameter with a regular expression in angled brackets:

@[regexp](code/javaguide/advanced/routing/JavaRoutingDsl.java)

In the above examples, the type of the parameters in the lambdas is undeclared, which the Java compiler defaults to `Object`.  The routing DSL in this case will pass the parameters as `String`, however if you define an explicit type on the parameter, the routing DSL will attempt to bind the parameter to that type:

@[integer](code/javaguide/advanced/routing/JavaRoutingDsl.java)

Supported types include `Integer`, `Long`, `Float`, `Double`, `Boolean`, and any type that extends [`PathBindable`](api/java/play/mvc/PathBindable.html).

Asynchronous actions are of course also supported, using the `routeAsync` method:

@[async](code/javaguide/advanced/routing/JavaRoutingDsl.java)

## Binding Routing DSL

Configuring an application to use a Routing DSL can be achieved in many ways, depending on use case:

### Embedding play

An example of embedding a play server with Routing DSL can be found in [[Embedding Play|JavaEmbeddingPlay]] section.

### Providing a DI router

A router can be provided to the application similarly as detailed in [[Application Entry point|ScalaCompileTimeDependencyInjection#Application-entry-point]] and [[Providing a router|ScalaCompileTimeDependencyInjection#Providing-a-router]], using e.g. a java builder class and an application loader:

@[load](code/AppLoader.java)

### Providing a DI router with Guice

A router via Guice could be provided with the following snippet:

@[load-guice2](code/GuiceRouterProvider.java)

and in the application loader:

@[load-guice1](code/GuiceAppLoader.java)

### Overriding binding

A router can also be provided using e.g. GuiceApplicationBuilder in the application loader to override with custom router binding or module as detailed in [[Bindings and Modules|JavaTestingWithGuice#Override-bindings]] 
