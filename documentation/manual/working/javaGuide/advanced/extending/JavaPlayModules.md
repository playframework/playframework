<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Writing Play Modules

> **Note:** This page covers the new [[module system|JavaDependencyInjection#Play-libraries]] to add new functionality to Play.
>
> The deprecated `play.Plugin` system is removed as of 2.5.x.  

A [[module|JavaDependencyInjection#Play-libraries]] can be written using any dependency injection framework.  However, when you want to extend Play, you need to avoid dependencies on a specific framework so that your extension can work independently of the dependency injection.  Play previously used the `play.Plugin` system for this purpose, but in 2.5.x, Plugins have been replaced with Play modules.
 
 A Play module is a class that extends [`play.inject.Module`](api/java/play/inject/Module.html) and can be registered with Play without relying explicitly on a particular dependency injection framework.  This allows everyone to use Play modules.

A list of Play modules are available in the [[module directory|ModuleDirectory]].

In addition, because Play uses Play modules for built-in functionality, a Play module must be used to replace or augment built-in functionality.

## Creating and migrating Play modules

Creating a Play module is simple:

@[module-class-api](code/javaguide/advanced/extending/MyApi.java)

@[module-class-definition](code/javaguide/advanced/extending/MyModule.java)

For more information, see the "Create a Module class" section of [[Plugins to Modules|PluginsToModules#Create-a-Module-class]].

## Module registration

By default, Play will load any class called `Module` that is defined in the root package (the "app" directory) or
you can define them explicitly inside the `reference.conf` or the `application.conf`:

```
play.modules.enabled += "modules.MyModule"
```

Please see [[migration page|PluginsToModules#Wire-it-up]] and [[the dependency injection documentation|JavaDependencyInjection#Play-libraries]] for more details.

## Application lifecycle

A module can detect when Play shutdown occurs by injecting the [`play.inject.ApplicationLifecycle`](api/java/play/inject/ApplicationLifecycle.html) interface into the singleton instance and adding a shutdown hook.

Please see the [[`ApplicationLifecycle` example|PluginsToModules#Create-a-Module-class]] and [[ApplicationLifecycle reference|JavaDependencyInjection#Stopping/cleaning-up]] for more details.

## Testing Play modules

Play Modules can be tested using Play's built in test functionality, by using the [`GuiceApplicationBuilder`](api/java/play/inject/guice/GuiceApplicationBuilder.html) and adding a binding to the module. 

@[module-class-binding](code/javaguide/advanced/extending/JavaExtendingPlay.java)

Please see [[Testing with Guice|JavaTestingWithGuice#Bindings-and-Modules]] for more details.

## Listing existing Play modules

The [`Modules.locate(env, conf)`](api/scala/play/api/inject/Modules$.html) method will display a list of all available Play modules in an application.  There is no corresponding Java class.

## Overriding Built-In Modules

There are some cases where Play provides a built-in module that must be overridden.  

For example, [[WSClient|JavaWS]] functionality is implemented by the [WSClient interface](api/java/play/libs/ws/WSClient.html) and backed by [AhcWSClientProvider](api/java/play/libs/ws/ahc/AhcWSModule.AhcWSClientProvider.html).  If you write a replacement class `MyWSClient` that extends `WSClient`, you can bind it with:

@[builtin-module-definition](code/javaguide/advanced/extending/MyWSModule.java)

### Testing Overrides

Testing the application should be done using the `overrides` method to replace the existing implementation: 

@[builtin-module-overrides](code/javaguide/advanced/extending/JavaExtendingPlay.java)

### Registration Overrides

Because the `AhcWSModule` is loaded automatically in `reference.conf`, you must first [[disable|JavaDependencyInjection#Excluding-modules]] the default module before adding the replacement module:

```
play.modules.disabled += "play.libs.ws.ahc.AhcWSModule"
play.modules.enabled += "modules.MyWSModule"
```

You should not disable existing modules in `reference.conf` when publishing a Play module, as it may have unexpected consequences.  Please see the [[migration page|PluginsToModules#Wire-it-up]] for details.
