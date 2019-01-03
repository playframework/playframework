<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Application Settings

A running instance of Play is built around the `Application` class, the starting point for most of the application state for Play.  The Application is loaded through an `ApplicationLoader` and is configured with a disposable classloader so that changing a setting in development mode will reload the Application.  Most of the `Application` settings are configurable, but more complex behavior can be hooked into Play by binding the various handlers to a specific instance through dependency injection.

* [[Essential Actions|JavaEssentialAction]]
* [[HTTP filters|JavaHttpFilters]]
* [[Error handling|JavaErrorHandling]]
