<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Writing Play Modules

> **Note:** This page covers the new [[module system|ScalaDependencyInjection#Play-libraries]] to add functionality to Play.
>
> The deprecated `play.api.Plugin` system is removed as of 2.5.x.  

A [[module|ScalaDependencyInjection#Play-libraries]] can be written using any dependency injection framework.  However, when you want to extend Play, you need to avoid dependencies on a specific framework so that your extension can work independently of the dependency injection.  Play previously used the `play.api.Plugin` system for this purpose, but in 2.5.x, Plugins have been replaced with Play modules.

A Play module is a class that extends [`play.api.inject.Module`](api/scala/play/api/inject/Module.html) and can be registered with Play without relying explicitly on a particular dependency injection framework.  This allows everyone to use Play modules.

A list of Play modules are available in the [[module directory|ModuleDirectory]].

In addition, because Play uses Play modules for built-in functionality, a Play module must be used to replace or augment built-in functionality.

## Creating and migrating Play modules

Creating a Play module is simple: 

@[module-definition](code/ScalaExtendingPlay.scala)

For more information, see the "Create a Module class" section of [[Plugins to Modules|PluginsToModules#Create-a-Module-class]].

## Module Registration

By default, Play will load any class called `Module` that is defined in the root package (the "app" directory) or
you can define them explicitly inside the `reference.conf` or the `application.conf`:

```
play.modules.enabled += "modules.MyModule"
```

Please see [[migration page|PluginsToModules#Wire-it-up]] and [[the dependency injection documentation|ScalaDependencyInjection#Play-libraries]] for more details.

## Application Lifecycle

A module can detect when Play shutdown occurs by injecting the [`play.api.inject.ApplicationLifecycle`]((api/scala/play/api/inject/ApplicationLifecycle.html) trait into the singleton instance and adding a shutdown hook.

Please see the [[`ApplicationLifecycle` example|PluginsToModules#Create-a-Module-class]] and [[ApplicationLifecycle reference|ScalaDependencyInjection#Stopping/cleaning-up]] for more details.

## Testing Play Modules

Modules can be tested using Play's built in test functionality, using the `GuiceApplicationBuilder` and adding a binding to the module. 

@[module-bindings](code/ScalaExtendingPlay.scala)

Please see [[Testing with Guice|ScalaTestingWithGuice#Bindings-and-Modules]] for more details.

## Listing Existing Play Modules

The [`Modules.locate(env, conf)`](api/scala/play/api/inject/Modules$.html) method will display a list of all available Play modules in an application.

## Overriding Built-In Modules

There are some cases where Play provides a built-in module that must be overridden.  

For example, [[Messages|ScalaI18N]] functionality is implemented by the [MessagesApi trait](api/scala/play/api/i18n/MessagesApi.html) and backed by [DefaultMessagesApi](api/scala/play/api/i18n/DefaultMessagesApi.html).  If you write a replacement class `MyMessagesApi` that extends `MessagesApi`, you can bind it with:

@[builtin-module-definition](code/ScalaExtendingPlay.scala)

### Testing Overrides

Testing the application should be done using the `overrides` method to replace the existing implementation: 

@[builtin-module-overrides](code/ScalaExtendingPlay.scala)

### Registration Overrides

Because the `I18nModule` is loaded automatically in `reference.conf`, you must first [[disable|ScalaDependencyInjection#Excluding-modules]] the default module before adding the replacement module:

```
play.modules.disabled += "play.api.i18n.I18nModule"
play.modules.enabled += "modules.MyI18nModule"
```

You should not disable existing modules in `reference.conf` when publishing a Play module, as it may have unexpected consequences.  Please see the [[migration page|PluginsToModules#Wire-it-up]] for details.
