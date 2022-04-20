<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# Compile Time Dependency Injection

Out of the box, Play provides a mechanism for runtime dependency injection - that is, dependency injection where dependencies aren't wired until runtime.  This approach has both advantages and disadvantages, the main advantages being around minimization of boilerplate code, the main disadvantage being that the construction of the application is not validated at compile time.

An alternative approach that is popular in Scala development is to use compile time dependency injection.  At its simplest, compile time DI can be achieved by manually constructing and wiring dependencies.  Other more advanced techniques and tools exist, such as macro based autowiring tools, implicit auto wiring techniques, and various forms of the cake pattern.  All of these can be easily implemented on top of constructors and manual wiring, so Play's support for compile time dependency injection is provided by providing public constructors and factory methods as API.

In addition to providing public constructors and factory methods, all of Play's out of the box modules provide some traits that implement a lightweight form of the cake pattern, for convenience.  These are built on top of the public constructors, and are completely optional.  In some applications, they will not be appropriate to use, but in many applications, they will be a very convenient mechanism to wiring the components provided by Play.  These traits follow a naming convention of ending the trait name with `Components`, so for example, the default HikariCP based implementation of the DB API provides a trait called [HikariCPComponents](api/scala/play/api/db/HikariCPComponents.html).

If you're new to compile-time DI or DI in general, it's worth reading Adam Warski's [guide to DI in Scala](https://di-in-scala.github.io/) that discusses compile-time DI in general and some of the helpers provided by his [Macwire](https://github.com/adamw/macwire) library.  This approach is easy to integrate with the built-in components traits provided by Play.

In the examples below, we will show how to wire a Play application manually using the built-in component helper traits.  By reading the source code of the provided component traits it should be trivial to adapt this to other dependency injection techniques as well.

## Application entry point

Every application that runs on the JVM needs an entry point that is loaded by reflection - even if your application starts itself, the main class is still loaded by reflection, and its main method is located and invoked using reflection.

In Play's dev mode, the JVM and HTTP server used by Play must be kept running between restarts of your application.  To implement this, Play provides an [ApplicationLoader](api/scala/play/api/ApplicationLoader.html) trait that you can implement.  The application loader is constructed and invoked every time the application is reloaded, to load the application.

This trait's load method takes as an argument the application loader [Context](api/scala/play/api/ApplicationLoader$$Context.html), which contains all the components required by a Play application that outlive the application itself and cannot be constructed by the application itself.  A number of these components exist specifically for the purposes of providing functionality in dev mode, for example, the source mapper allows the Play error handlers to render the source code of the place that an exception was thrown.

The simplest implementation of this can be provided by extending the Play [BuiltInComponentsFromContext](api/scala/play/api/BuiltInComponentsFromContext.html) abstract class.  This class takes the context, and provides all the built in components, based on that context.  The only thing you need to provide is a router for Play to route requests to.  Below is the simplest application that can be created in this way, using a null router:

@[basic](code/CompileTimeDependencyInjection.scala)

To configure Play to use this application loader, configure the `play.application.loader` property to point to the fully qualified class name in the `application.conf` file:

    play.application.loader=MyApplicationLoader

In addition, if you're modifying an existing project that uses the built-in Guice module, you should be able to remove `guice` from your `libraryDependencies` in `build.sbt`.

## Configuring Logging

To correctly configure logging in Play, the `LoggerConfigurator` must be run before the application is returned.  The default  [BuiltInComponentsFromContext](api/scala/play/api/BuiltInComponentsFromContext.html) does not call `LoggerConfigurator` for you.

This initialization code must be added in your application loader:

@[basicextended](code/CompileTimeDependencyInjection.scala)

If you are migrating from Play 2.4.x, `LoggerConfigurator` is the replacement for `Logger.configure()` and allows for [[customization of different logging frameworks|SettingsLogger#Using-a-Custom-Logging-Framework]].

## Providing a router

By default Play will use the [[injected routes generator|ScalaDependencyInjection#Injected-routes-generator]]. This generates a router with a constructor that accepts each of the controllers and included routers from your routes file, in the order they appear in your routes file.  The router's constructor will also, as its first argument, accept an [`HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html), which is used to handle parameter binding errors, and a prefix String as its last argument. An overloaded constructor that defaults this to `"/"` will also be provided.

The following routes:

@[content](code/scalaguide.dependencyinjection.routes)

Will produce a router with the following constructor signatures:

```scala
class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler,
  Application_0: controllers.Application,
  bar_Routes_0: bar.Routes,
  Assets_1: controllers.Assets,
  val prefix: String
) extends GeneratedRouter {

  def this(
    errorHandler: play.api.http.HttpErrorHandler,
    Application_0: controllers.Application,
    bar_Routes_0: bar.Routes,
    Assets_1: controllers.Assets
  ) = this(Application_0, bar_Routes_0, Assets_1, "/")
  ...
}
```

Note that the naming of the parameters is intentionally not well defined (and in fact the index that is appended to them is random, depending on hash map ordering), so you should not depend on the names of these parameters.

To use this router in an actual application:

@[routers](code/CompileTimeDependencyInjection.scala)

## Using other components

As described before, Play provides a number of helper traits for wiring in other components.  For example, if you wanted to use the messages module, you can mix in [I18nComponents](api/scala/play/api/i18n/I18nComponents.html) into your components cake, like so:

@[messages](code/CompileTimeDependencyInjection.scala)

Other helper traits are also available as the [CSRFComponents](api/scala/play/filters/csrf/CSRFComponents.html) or the [AhcWSComponents](api/scala/play/api/libs/ws/ahc/AhcWSComponents.html)
