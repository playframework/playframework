<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Runtime Dependency Injection

Dependency injection is a way that you can separate your components so that they are not directly dependent on each other, rather, they get injected into each other.

Out of the box, Play provides runtime dependency injection based on [JSR 330](https://jcp.org/en/jsr/detail?id=330).  Runtime dependency injection is so called because the dependency graph is created, wired and validated at runtime.  If a dependency cannot be found for a particular component, you won't get an error until you run your application.  In contrast, Play also supports [[compile time dependency injection|ScalaCompileTimeDependencyInjection]], where errors in the dependency graph are detected and thrown at compile time.

The default JSR 330 implementation that comes with Play is [Guice](https://github.com/google/guice), but other JSR 330 implementations can be plugged in.

## Declaring dependencies

If you have a component, such as a controller, and it requires some other components as dependencies, then this can be declared using the [@Inject](https://docs.oracle.com/javaee/7/api/javax/inject/Inject.html) annotation.  The `@Inject` annotation can be used on fields or on constructors, we recommend that you use it on constructors, for example:

@[constructor](code/RuntimeDependencyInjection.scala)

Note that the `@Inject` annotation must come after the class name but before the constructor parameters, and must have parenthesis.

## Dependency injecting controllers

There are two ways to make Play use dependency injected controllers.

### Injected routes generator

By default, Play will generate a static router, that assumes that all actions are static methods.  By configuring Play to use the injected routes generator, you can get Play to generate a router that will declare all the controllers that it routes to as dependencies, allowing your controllers to be dependency injected themselves.

We recommend always using the injected routes generator, the static routes generator exists primarily as a tool to aid migration so that existing projects don't have to make all their controllers non static at once.

To enable the injected routes generator, add the following to your build settings in `build.sbt`:

@[content](code/injected.sbt)

When using the injected routes generator, prefixing the action with an `@` symbol takes on a special meaning, it means instead of the controller being injected directly, a `Provider` of the controller will be injected.  This allows, for example, prototype controllers, as well as an option for breaking cyclic dependencies.

### Injected actions

If using the static routes generator, you can indicate that an action has an injected controller by prefixing the action with `@`, like so:

@[injected](code/scalaguide.advanced.dependencyinjection.injected.routes)

## Component lifecycle

The dependency injection system manages the lifecycle of injected components, creating them as needed and injecting them into other components. Here's how component lifecycle works:

* **New instances are created every time a component is needed**. If a component is used more than once, then, by default, multiple instances of the component will be created. If you only want a single instance of a component then you need to mark it as a [singleton](#Singletons).
* **Instances are created lazily when they are needed**. If a component is never used by another component, then it won't be created at all. This is usually what you want. For most components there's no point creating them until they're needed. However, in some cases you want components to be started up straight away or even if they're not used by another component. For example, you might want to send a message to a remote system or warm up a cache when the application starts. You can force a component to be created eagerly by using an [eager binding](#Eager-bindings).
* **Instances are _not_ automatically cleaned up**, beyond normal garbage collection. Components will be garbage collected when they're no longer referenced, but the framework won't do anything special to shut down the component, like calling a `close` method. However, Play provides a special type of component, called the `ApplicationLifecycle` which lets you register components to [shut down when the application stops](#Stopping/cleaning-up).

## Singletons

Sometimes you may have a component that holds some state, such as a cache, or a connection to an external resource, or a component might be expensive to create. In these cases it may be important that there is only be one instance of that component. This can be achieved using the [@Singleton](https://docs.oracle.com/javaee/7/api/javax/inject/Singleton.html) annotation:

@[singleton](code/RuntimeDependencyInjection.scala)

## Stopping/cleaning up

Some components may need to be cleaned up when Play shuts down, for example, to stop thread pools.  Play provides an [ApplicationLifecycle](api/scala/play/api/inject/ApplicationLifecycle.html) component that can be used to register hooks to stop your component when Play shuts down:

@[cleanup](code/RuntimeDependencyInjection.scala)

The `ApplicationLifecycle` will stop all components in reverse order from when they were created.  This means any components that you depend on can still safely be used in your components stop hook, since because you depend on them, they must have been created before your component was, and therefore won't be stopped until after your component is stopped.

> **Note:** It's very important to ensure that all components that register a stop hook are singletons.  Any non singleton components that register stop hooks could potentially be a source of memory leaks, since a new stop hook will be registered each time the component is created.

## Providing custom bindings

It is considered good practice to define an trait for a component, and have other classes depend on that trait, rather than the implementation of the component.  By doing that, you can inject different implementations, for example you inject a mock implementation when testing your application.

In this case, the DI system needs to know which implementation should be bound to that trait.  The way we recommend that you declare this depends on whether you are writing a Play application as an end user of Play, or if you are writing library that other Play applications will consume.

### Play applications

We recommend that Play applications use whatever mechanism is provided by the DI framework that the application is using.  Although Play does provide a binding API, this API is somewhat limited, and will not allow you to take full advantage of the power of the framework you're using.

Since Play provides support for Guice out of the box, the examples below show how to provide bindings for Guice.

#### Binding annotations

The simplest way to bind an implementation to an interface is to use the Guice [@ImplementedBy](https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/ImplementedBy.html) annotation.  For example:

@[implemented-by](code/RuntimeDependencyInjection.scala)

#### Programmatic bindings

In some more complex situations, you may want to provide more complex bindings, such as when you have multiple implementations of the one trait, which are qualified by [@Named](https://docs.oracle.com/javaee/7/api/javax/inject/Named.html) annotations.  In these cases, you can implement a custom Guice [Module](https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Module.html):

@[guice-module](code/RuntimeDependencyInjection.scala)

To register this module with Play, append it's fully qualified class name to the `play.modules.enabled` list in `application.conf`:

    play.modules.enabled += "modules.HelloModule"

#### Configurable bindings

Sometimes you might want to read the Play `Configuration` or use a `ClassLoader` when you configure Guice bindings. You can get access to these objects by adding them to your module's constructor.

In the example below, the `Hello` binding for each language is read from a configuration file. This allows new `Hello` bindings to be added by adding new settings in your `application.conf` file.

@[dynamic-guice-module](code/RuntimeDependencyInjection.scala)

> **Note:** In most cases, if you need to access `Configuration` when you create a component, you should inject the `Configuration` object into the component itself or into the component's `Provider`. Then you can read the `Configuration` when you create the component. You usually don't need to read `Configuration` when you create the bindings for the component.

#### Eager bindings

In the code above, new `EnglishHello` and `GermanHello` objects will be created each time they are used. If you only want to create these objects once, perhaps because they're expensive to create, then you should use the `@Singleton` annotation as [described above](#Singletons). If you want to create them once and also create them _eagerly_ when the application starts up, rather than lazily when they are needed, then you can [Guice's eager singleton binding](https://github.com/google/guice/wiki/Scopes#eager-singletons).

@[eager-guice-module](code/RuntimeDependencyInjection.scala)

Eager singletons can be used to start up a service when an application starts. They are often combined with a [shutdown hook](#Stopping/cleaning-up) so that the service can clean up its resources when the application stops.

### Play libraries

If you're implementing a library for Play, then you probably want it to be DI framework agnostic, so that your library will work out of the box regardless of which DI framework is being used in an application.  For this reason, Play provides a lightweight binding API for providing bindings in a DI framework agnostic way.

To provide bindings, implement a [Module](api/scala/play/api/inject/Module.html) to return a sequence of the bindings that you want to provide.  The `Module` trait also provides a DSL for building bindings:

@[play-module](code/RuntimeDependencyInjection.scala)

This module can be registered with Play automatically by appending it to the `play.modules.enabled` list in `reference.conf`:

    play.modules.enabled += "com.example.HelloModule"

* The `Module` `bindings` method takes a Play `Environment` and `Configuration`. You can access these if you want to [configure the bindings dynamically](#Configurable-bindings).
* Module bindings support [eager bindings](#Eager-bindings). To declare an eager binding, add `.eagerly` at the end of your `Binding`.

In order to maximise cross framework compatibility, keep in mind the following things:

* Not all DI frameworks support just in time bindings. Make sure all components that your library provides are explicitly bound.
* Try to keep binding keys simple - different runtime DI frameworks have very different views on what a key is and how it should be unique or not.

### Excluding modules

If there is a module that you don't want to be loaded, you can exclude it by appending it to the `play.modules.disabled` property in `application.conf`:

    play.modules.disabled += "play.api.db.evolutions.EvolutionsModule"

## Advanced: Extending the GuiceApplicationLoader

Play's runtime dependency injection is bootstrapped by the [`GuiceApplicationLoader`](api/scala/play/api/inject/guice/GuiceApplicationLoader.html) class. This class loads all the modules, feeds the modules into Guice, then uses Guice to create the application. If you want to control how Guice initializes the application then you can extend the `GuiceApplicationLoader` class.

There are several methods you can override, but you'll usually want to override the `builder` method. This method reads the [`ApplicationLoader.Context`](api/scala/play/api/ApplicationLoader$$Context.html) and creates a [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html). Below you can see the standard implementation for `builder`, which you can change in any way you like. You can find out how to use the `GuiceApplicationBuilder` in the section about [[testing with Guice|ScalaTestingWithGuice]].

@[custom-application-loader](code/RuntimeDependencyInjection.scala)

When you override the [`ApplicationLoader`](api/scala/play/api/ApplicationLoader.html) you need to tell Play. Add the following setting to your `application.conf`:

    play.application.loader = "modules.CustomApplicationLoader"

You're not limited to using Guice for dependency injection. By overriding the `ApplicationLoader` you can take control of how the application is initialized. Find out more in the [[next section|ScalaCompileTimeDependencyInjection]].