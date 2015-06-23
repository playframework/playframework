<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Migrating Plugin to Module

If you have implemented a Play plugin, please consider migrating your implementation to use [`play.api.inject.Module`](api/scala/play/api/inject/Module.html), instead of the deprecated Java [`play.Plugin`](api/java/play/Plugin.html) or Scala [`play.api.Plugin`](api/scala/play/api/Plugin.html) types.

The main difference between the old `Plugin` API, and the new one using `Module`, is that with the latter we are going to fully embrace Dependency Injection (DI) - you can read [[here|Highlights24#Dependency-Injection]] to understand why Play became opinionated about DI.

## Rationale

With the old `Plugin` API, you were required to provide a `play.plugins` file containing a number and the fully qualified name of the plugin to load. The number was used by Play to determine a total order between plugins. This approach was brittle, as it was difficult to ensure that the number corresponded to the right place in the initialization order, so the plugin's dependencies were initialized before the plugin was initialized. Furthermore, there was no way to avoid that two plugins would use the same number. Both problems have been solved by using DI. Components now explicitly declare their dependencies as constructor arguments and do their initialization in the constructor.

## Create a `Module` class

Start by creating a class that inherits from `play.api.inject.Module`, and provide an implementation for the `bindings` method. In this method you should wire types to concrete implementation so that components provided by your module can be injected in users' code, or in other modules. Next follows an example.

In Java

@[module-decl](code24/MyModule.java)

In Scala

@[module-decl](code24/MyModule.scala)


Note that if a component you are defining requires another component, you should simply add the required component as a constructor's dependency, prepending the constructor with the `@javax.inject.Inject` annotation. The DI framework will then take care of the rest.

> Note that if a component B requires A, then B will be initialized only after A is initialized.

Next follows an example of a component named `MyComponentImpl` requiring the `ApplicationLifecycle` component.

In Java

@[components-decl](code24/MyComponent.java)

In Scala

@[components-decl](code24/MyComponent.scala)

## Wire it up

Now it's time to add your freshly created `Module` class to the set of enabled modules. Doing so is as simple as adding the following line in your configuration file:

```conf
play.modules.enabled  += "my.module.MyModule"
```

If you are working on a library that will be used by other projects (including sub projects), add the above line in your `reference.conf` file (if you don't have a `reference.conf` yet, create one and place it under `src/main/resources`). Otherwise, if it's in an end Play project, it should be in `application.conf`.

> Note: If you are working on a library, it is highly discouraged to use `play.modules.disabled` to disable modules, as it can lead to undetermistic results when modules are loaded by the application (see [this issue](https://github.com/playframework/play-slick/issues/245) for reasons on why you should not touch `play.modules.disabled`). In fact, `play.modules.disabled` is intended for end users to be able to override what modules are enabled by default.

### Compile-time DI

By defining a `Module` class, you have made your component usable with runtime DI frameworks such a Google Guice or Spring. An alternative that is popular in Scala is [[compile-time DI|ScalaCompileTimeDependencyInjection#Compile-Time-Dependency-Injection]]. To make your component usable also with compile-time DI provide a Scala `trait` that declares the component's dependencies. Here is how you would do it for the running example:

@[components-decl](code24/MyModule.scala)

## Delete your `Plugin` class and `play.plugins` file

At this point, you should have successfully migrated your code, hence it's time to delete both your `Plugin` class and the `play.plugins` file. The latter is typically located in your project's `conf` folder.
