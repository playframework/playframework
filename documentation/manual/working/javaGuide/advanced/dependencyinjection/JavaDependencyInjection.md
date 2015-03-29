<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Dependency Injection

Dependency injection is a way that you can separate your components so that they are not directly dependent on each other, rather, they get injected into each other.

Out of the box, Play provides dependency injection support based on [JSR 330](https://jcp.org/en/jsr/detail?id=330).  The default JSR 330 implementation that comes with Play is [Guice](https://github.com/google/guice), but other JSR 330 implementations can be plugged in.

## Declaring dependencies

If you have a component, such as a controller, and it requires some other components as dependencies, then this can be declared using the [@Inject](http://docs.oracle.com/javaee/6/api/javax/inject/Inject.html) annotation.  The `@Inject` annotation can be used on fields or on constructors, which you decide to use is up to you.  For example, to use field injection:

@[field](code/javaguide/advanced/di/field/MyComponent.java)

To use constructor injection:

@[constructor](code/javaguide/advanced/di/constructor/MyComponent.java)

Each of these have their own benefits, and which is best is a matter of hot debate.  For brevity, in the Play documentation, we use field injection, but in Play itself, we use constructor injection.

## Singletons

Sometimes you may have a component that holds some state, such as a cache, or a connection to an external resource.  In this case it may be important that there only be one of that component.  This can be achieved using the [@Singleton](http://docs.oracle.com/javaee/6/api/javax/inject/Singleton.html) annotation:

@[singleton](code/javaguide/advanced/di/CurrentSharePrice.java)

## Stopping/cleaning up

Some components may need to be cleaned up when Play shuts down, for example, to stop thread pools.  Play provides an [ApplicationLifecycle](api/java/play/inject/ApplicationLifecycle.html) component that can be used to register hooks to stop your component when Play shuts down:

@[cleanup](code/javaguide/advanced/di/MessageQueueConnection.java)


The `ApplicationLifecycle` will stop all components in reverse order from when they were created.  This means any components that you depend on can still safely be used in your components stop hook, since because you depend on them, they must have been created before your component was, and therefore won't be stopped until after your component is stopped.

> **Note:** It's very important to ensure that all components that register a stop hook are singletons.  Any non singleton components that register stop hooks could potentially be a source of memory leaks, since a new stop hook will be registered each time the component is created.

## Providing custom bindings

It is considered good practice to define an interface for a component, and have other classes depend on that interface, rather than the implementation of the component.  By doing that, you can inject different implementations, for example you inject a mock implementation when testing your application.

In this case, the DI system needs to know which implementation should be bound to that interface.  The way we recommend that you declare this depends on whether you are writing a Play application as an end user of Play, or if you are writing library that other Play applications will consume.

### Play applications

We recommend that Play applications use whatever mechanism is provided by the DI framework that the application is using.  Although Play does provide a binding API, this API is somewhat limited, and will not allow you to take full advantage of the power of the framework you're using.

Since Play provides support for Guice out of the box, the examples below show how to provide bindings for Guice.

The simplest way to bind an implementation to an interface is to use the Guice [@ImplementedBy](http://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/ImplementedBy.html) annotation.  For example:

@[implemented-by](code/javaguide/advanced/di/Hello.java)
@[implemented-by](code/javaguide/advanced/di/EnglishHello.java)

In some more complex situations, you may want to provide more complex bindings, such as when you have multiple implementations of the one trait, which are qualified by [@Named](http://docs.oracle.com/javaee/6/api/javax/inject/Named.html) annotations.  In these cases, you can implement a custom Guice [Module](http://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Module.html):

@[guice-module](code/javaguide/advanced/di/guice/HelloModule.java)

To register this module with Play, append it's fully qualified class name to the `play.modules.enabled` list in `application.conf`:

    play.modules.enabled += "modules.HelloModule"

You can also provide your own application loader:

@[guice-app-loader](code/javaguide/advanced/di/guice/CustomApplicationLoader.java)

You then need to specifiy it in `play.application.loader`:

    play.application.loader := "modules.CustomApplicationLoader"

### Play libraries

If you're implementing a library for Play, then you probably want it to be DI framework agnostic, so that your library will work out of the box regardless of which DI framework is being used in an application.  For this reason, Play provides a lightweight binding API for providing bindings in a DI framework agnostic way.

To provide bindings, implement a [Module](api/scala/index.html#play.api.inject.Module) to return a sequence of the bindings that you want to provide.  The `Module` trait also provides a DSL for building bindings:

@[play-module](code/javaguide/advanced/di/play/HelloModule.java)

This module can be registered with Play automatically by appending it to the `play.modules.enabled` list in `reference.conf`:

    play.modules.enabled += "com.example.HelloModule"

In order to maximise cross framework compatibility, keep in mind the following things:

* Not all DI frameworks support just in time bindings. Make sure all components that your library provides are explicitly bound.
* Try to keep binding keys simple - different runtime DI frameworks have very different views on what a key is and how it should be unique or not.

## Excluding modules

If there is a module that you don't want to be loaded, you can exclude it by appending it to the `play.modules.disabled` property in `application.conf`:

    play.modules.disabled += "play.api.db.evolutions.EvolutionsModule"
