<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Application Settings

A running instance of Play is built around the `Application` class, the starting point for most of the application state for Play.  The Application is loaded through an `ApplicationLoader` and is configured with a disposable classloader so that changing a setting in development mode will reload the Application.  Most of the `Application` settings are configurable, but more complex behavior can be hooked into Play by binding the various handlers to a specific instance through dependency injection.

> **Note:** Application configuration has changed in Play 2.5.x so that [[dependency injection|JavaDependencyInjection]] is the primary method of configuration.
>
> Configuring the application through `GlobalSettings` class is still available through [[Global Settings|JavaGlobal]], but is deprecated and may be removed in future versions.  Please see the [[Removing `GlobalSettings`|GlobalSettings]] page for how to migrate away from GlobalSettings.

* [[Essential Actions|JavaEssentialAction]]
* [[HTTP filters|JavaHttpFilters]]
* [[Error handling|JavaErrorHandling]]
* [[Global settings|JavaGlobal]]
