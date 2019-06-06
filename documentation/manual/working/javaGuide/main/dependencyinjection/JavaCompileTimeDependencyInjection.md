<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Compile Time Dependency Injection

Out of the box, Play provides a mechanism for runtime dependency injection - that is, dependency injection where dependencies aren't wired until runtime.  This approach has both advantages and disadvantages, the main advantages being around minimization of boilerplate code, the main disadvantage being that the construction of the application is not validated at compile time.

An alternative approach is to use compile time dependency injection.  At its simplest, compile time DI can be achieved by manually constructing and wiring dependencies.  Other more advanced techniques and tools exist, such as [Dagger](https://google.github.io/dagger/).  All of these can be easily implemented on top of constructors and manual wiring, so Play's support for compile time dependency injection is provided by providing public constructors and factory methods as API.

> **Note**: If you're new to compile-time DI or DI in general, it's worth reading Adam Warski's [guide to DI in Scala](https://di-in-scala.github.io/) that discusses compile-time DI in general. While this is an explanation for Scala developers, it could also give you some insights about the advantages of Compile Time Injection.

In addition to providing public constructors and factory methods, all of Play's out of the box modules provide some interface that implement a lightweight form of the cake pattern, for convenience.  These are built on top of the public constructors, and are completely optional.  In some applications, they will not be appropriate to use, but in many applications, they will be a very convenient mechanism to wiring the components provided by Play.  These interfaces follow a naming convention of ending the trait name with `Components`, so for example, the default HikariCP based implementation of the DB API provides a interface called [HikariCPComponents](api/java/play/db/HikariCPComponents.html).

> **Note**: Of course, Java has some limitations to fully implement the cake pattern. For example, you can't have state in interfaces.

In the examples below, we will show how to wire a Play application manually using the built-in component helper interfaces.  By reading the source code of the provided component interfaces it should be trivial to adapt this to other dependency injection techniques as well.

## Application entry point

Every application that runs on the JVM needs an entry point that is loaded by reflection - even if your application starts itself, the main class is still loaded by reflection, and its main method is located and invoked using reflection.

In Play's dev mode, the JVM and HTTP server used by Play must be kept running between restarts of your application.  To implement this, Play provides an [ApplicationLoader](api/java/play/ApplicationLoader.html) interface that you can implement.  The application loader is constructed and invoked every time the application is reloaded, to load the application.

This interfaces's load method takes as an argument the application loader [Context](api/java/play/ApplicationLoader.Context.html), which contains all the components required by a Play application that outlive the application itself and cannot be constructed by the application itself.  A number of these components exist specifically for the purposes of providing functionality in dev mode, for example, the source mapper allows the Play error handlers to render the source code of the place that an exception was thrown.

The simplest implementation of this can be provided by extending the Play [BuiltInComponentsFromContext](api/java/play/BuiltInComponentsFromContext.html) abstract class.  This class takes the context, and provides all the built in components, based on that context.  The only thing you need to provide is a router for Play to route requests to.  Below is the simplest application that can be created in this way, using an empty router:

@[basic-imports](code/javaguide/di/components/CompileTimeDependencyInjection.java)

@[basic-my-components](code/javaguide/di/components/CompileTimeDependencyInjection.java)

And then the application loader:

@[basic-app-loader](code/javaguide/di/components/CompileTimeDependencyInjection.java)

To configure Play to use this application loader, configure the `play.application.loader` property to point to the fully qualified class name in the `application.conf` file:

    play.application.loader=MyApplicationLoader

In addition, if you're modifying an existing project that uses the built-in Guice module, you should be able to remove `guice` from your `libraryDependencies` in `build.sbt`.

## Providing a Router

To configure a router, you have two options, use [`RoutingDsl`](api/java/play/routing/RoutingDsl.html) or the generated router.

### Providing a router with `RoutingDsl`

To make this easier, Play has a [`RoutingDslComponentsFromContext`](api/java/play/routing/RoutingDslComponentsFromContext.html) class that already provides a `RoutingDsl` instance, created using the other provided components:

@[with-routing-dsl](code/javaguide/di/components/CompileTimeDependencyInjection.java)

### Using the generated router

By default Play will use the [[injected routes generator|JavaDependencyInjection#Injected-routes-generator]]. This generates a router with a constructor that accepts each of the controllers and included routers from your routes file, in the order they appear in your routes file.  The router's constructor will also, as its first argument, accept an [`play.api.http.HttpErrorHandler`](api/scala/play/api/http/HttpErrorHandler.html) (the Scala version of [`play.http.HttpErrorHandler`](api/java/play/http/HttpErrorHandler.html)), which is used to handle parameter binding errors, and a prefix String as its last argument. An overloaded constructor that defaults this to `"/"` will also be provided.

The following routes:

@[content](code/javaguide.dependencyinjection.routes)

Will produce a router that accepts instances of `controllers.HomeController`, `controllers.Assets` and any other that has a declared route. To use this router in an actual application:

@[with-generated-router](code/javaguide/di/components/CompileTimeDependencyInjection.java)

## Configuring Logging

To correctly configure logging in Play, the `LoggerConfigurator` must be run before the application is returned.  The default [BuiltInComponentsFromContext](api/java/play/BuiltInComponentsFromContext.html) does not call `LoggerConfigurator` for you.

This initialization code must be added in your application loader:

@[basic-logger-configurator](code/javaguide/di/components/CompileTimeDependencyInjection.java)

## Using other components

As described before, Play provides a number of helper traits for wiring in other components.  For example, if you wanted to use a database connection pool, you can mix in [HikariCPComponents](api/java/play/db/HikariCPComponents.html) into your components cake, like so:

@[connection-pool](code/javaguide/di/components/CompileTimeDependencyInjection.java)

Other helper traits are also available as the [CSRFComponents](api/java/play/filters/components/CSRFComponents.html) or the [AhcWSComponents](api/java/play/libs/ws/ahc/AhcWSComponents.html). The complete list of Java interfaces that provides components is:

- [`play.BuiltInComponents`](api/java/play/BuiltInComponents.html)
- [`play.components.AkkaComponents`](api/java/play/components/AkkaComponents.html)
- [`play.components.ApplicationComponents`](api/java/play/components/ApplicationComponents.html)
- [`play.components.BaseComponents`](api/java/play/components/BaseComponents.html)
- [`play.components.BodyParserComponents`](api/java/play/components/BodyParserComponents.html)
- [`play.components.ConfigurationComponents`](api/java/play/components/ConfigurationComponents.html)
- [`play.components.CryptoComponents`](api/java/play/components/CryptoComponents.html)
- [`play.components.FileMimeTypesComponents`](api/java/play/components/FileMimeTypesComponents.html)
- [`play.components.HttpComponents`](api/java/play/components/HttpComponents.html)
- [`play.components.HttpConfigurationComponents`](api/java/play/components/HttpConfigurationComponents.html)
- [`play.components.HttpErrorHandlerComponents`](api/java/play/components/HttpErrorHandlerComponents.html)
- [`play.components.TemporaryFileComponents`](api/java/play/components/TemporaryFileComponents.html)
- [`play.controllers.AssetsComponents`](api/java/play/controllers/AssetsComponents.html)
- [`play.i18n.I18nComponents`](api/java/play/i18n/I18nComponents.html)
- [`play.libs.ws.ahc.AhcWSComponents`](api/java/play/libs/ws/ahc/AhcWSComponents.html)
- [`play.libs.ws.ahc.WSClientComponents`](api/java/play/libs/ws/ahc/WSClientComponents.html)
- [`play.cache.ehcache.EhCacheComponents`](api/java/play/cache/ehcache/EhCacheComponents.html)
- [`play.filters.components.AllowedHostsComponents`](api/java/play/filters/components/AllowedHostsComponents.html)
- [`play.filters.components.CORSComponents`](api/java/play/filters/components/CORSComponents.html)
- [`play.filters.components.CSRFComponents`](api/java/play/filters/components/CSRFComponents.html)
- [`play.filters.components.GzipFilterComponents`](api/java/play/filters/components/GzipFilterComponents.html)
- [`play.filters.components.HttpFiltersComponents`](api/java/play/filters/components/HttpFiltersComponents.html)
- [`play.filters.components.NoHttpFiltersComponents`](api/java/play/filters/components/NoHttpFiltersComponents.html)
- [`play.filters.components.RedirectHttpsComponents`](api/java/play/filters/components/RedirectHttpsComponents.html)
- [`play.filters.components.SecurityHeadersComponents`](api/java/play/filters/components/SecurityHeadersComponents.html)
- [`play.routing.RoutingDslComponents`](api/java/play/routing/RoutingDslComponents.html)
- [`play.data.FormFactoryComponents`](api/java/play/data/FormFactoryComponents.html)
- [`play.data.validation.ValidatorsComponents`](api/java/play/data/validation/ValidatorsComponents.html)
- [`play.db.BoneCPComponents`](api/java/play/db/BoneCPComponents.html)
- [`play.db.ConnectionPoolComponents`](api/java/play/db/ConnectionPoolComponents.html)
- [`play.db.DBComponents`](api/java/play/db/DBComponents.html)
- [`play.db.HikariCPComponents`](api/java/play/db/HikariCPComponents.html)
- [`play.db.jpa.JPAComponents`](api/java/play/db/jpa/JPAComponents.html)
- [`play.libs.openid.OpenIdComponents`](api/java/play/libs/openid/OpenIdComponents.html)