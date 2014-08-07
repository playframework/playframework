# Compile Time Dependency Injection

Out of the box, Play provides a mechanism for runtime dependency injection - that is, dependency injection where dependencies aren't wired until runtime.  This approach has both advantages and disadvantages, the main advantages being around minimisation of boilerplate code, the main disadvantage being that the construction of the application is not validated at compile time.

An alternative approach that is popular in Scala development is to use compile time dependency injection.  At its simplest, compile time DI can be achieved by manually constructing and wiring dependencies.  Other more advanced techniques and tools exist, such as macro based autowiring tools, implicit auto wiring techniques, and various forms of the cake pattern.  All of these can be easily implemented on top of constructors and manual wiring, so Play's support for compile time dependency injection is provided by providing public constructors and factory methods as API.
  
In addition to providing public constructors and factory methods, all of Play's out of the box modules provide some traits that implement a lightweight form of the cake pattern, for convenience.  These are built on top of the public constructors, and are completely optional.  In some applications, they will not be appropriate to use, but in many applications, they will be a very convenient mechanism to wiring the components provided by Play.  These traits follow a naming convention of ending the trait name with `Components`, so for example, the default BoneCP based implementation of the DB API provides a trait called [BoneCPComponents](api/scala/index.html#play.api.db.BoneCPComponents).

In the examples below, we will show how to wire a Play application manually using the built in component helper traits.  By reading the source code of these traits it should be trivial to adapt this to any compile time dependency injection technique you please.

## Current application

One aim of dependency injection is to eliminate global state, such as singletons.  Play 2 was designed with an assumption of global state.  Play 3 will hopefully remove this global state, however that is a major breaking task.  In the meantime, Play will be a bit of a hybrid state, with some parts not using global state, and other parts using global state.

By using dependency injection throughout your application, you should be able to ensure though that your components can be tested in isolation, not requiring starting an entire application to run a single test.

## Application entry point

Every application that runs on the JVM needs an entry point that is loaded by reflection - even if your application starts itself, the main class is still loaded by reflection, and its main method is located and invoked using reflection.

In Play's dev mode, the JVM and HTTP server used by Play must be kept running between restarts of your application.  To implement this, Play provides an [ApplicationLoader](api/scala/index.html#play.api.ApplicationLoader) trait that you can implement.  The application loader is constructed and invoked every time the application is reloaded, to load the application.

This trait's load method takes as an argument the application loader [Context](api/scala/index.html#play.api.ApplicationLoader$$Context), which contains all the components required by a Play application that outlive the application itself and cannot be constructed by the application itself.  A number of these components exist specifically for the purposes of providing functionality in dev mode, for example, the source mapper allows the Play error handlers to render the source code of the place that an exception was thrown.

The simplest implementation of this can be provided by extending the Play [BuiltInComponentsFromContext](api/scala/index.html#play.api.BuiltInComponentsFromContext) abstract class.  This class takes the context, and provides all the built in components, based on that context.  The only thing you need to provide is a router for Play to route requests to.  Below is the simplest application that can be created in this way, using a null router:

@[basic](code/CompileTimeDependencyInjection.scala)

To configure Play to use this application loader, configure the `play.application.loader` property to point to the fully qualified class name in the `application.conf` file:

    play.application.loader=MyApplicationLoader
    
## Providing a router

This will be documented once we have a router that can be dependency injected.

## Using other components

As described before, Play provides a number of helper traits for wiring in other components.  For example, if you wanted to use the messages module, you can mix in [I18nComponents](api/scala/index.html#play.api.i18n.I18nComponents) into your components cake, like so:

@[messages](code/CompileTimeDependencyInjection.scala)
