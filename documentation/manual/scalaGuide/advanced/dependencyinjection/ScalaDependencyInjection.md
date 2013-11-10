# Dependency Injection

Play does not use any dependency injection framework under the hood, and leaves the choice of DI framework (if any) in your hands.  Models, Services and Configuration objects can all be handled transparently, and do not need explicit configuration to work in Play.

There is one case which requires explicit configuration, which involves how controllers (which are singleton objects by default) interact with routes.

## Controller Injection

In Play, routes which start with `@` are managed by [play.api.GlobalSettings#getControllerInstance](api/scala/index.html#play.api.GlobalSettings),

Given the following route definition:

@[di-routes](code/scalaguide.advanced.dependencyinjection.routes)

Then you can manage controller class instantiation using a DI framework by overriding `getControllerInstance` in your application's `Global` class:

@[di-global](code/ControllerInjection.scala)

## Example Projects

The pace of development and the myriad of options even within a single DI framework means that full documentation is beyond the scope of this documentation.  However, there are a number of sample projects that demonstrate how to best leverage DI in your project.

### Activator

[Typesafe Activator](http://www.typesafe.com/activator) is a local web & command-line tool that helps developers get started with the Typesafe Platform.  Using Activator is highly recommended, as you can download a number of sample projects at once and walk through tutorials presented through the Activator UI.

### Spring

[Spring](http://www.springsource.org/) is a popular application development for Java that has a dependency injection framework at its core.  There is also an additional project [Spring Scala](https://github.com/SpringSource/spring-scala), which provides additional integration options using Scala and Spring.

There is an [Activator](http://www.typesafe.com/activator) project available for Spring.  You can download it from Activator [directly](http://typesafe.com/activator/template/play-spring-data-jpa), or clone it from [https://github.com/typesafehub/play-spring-data-jpa](https://github.com/typesafehub/play-spring-data-jpa).

### Subcut

[Subcut](https://github.com/dickwall/subcut/blob/master/GettingStarted.md) is a lightweight dependency injection framework written for Scala that uses implicits to pass configuration through injectable classes.

There is a Github project by the Subcut team that shows how to integrate Subcut with Play.  You can clone it from [https://github.com/dickwall/play-subcut](https://github.com/dickwall/play-subcut) and it is also an Activator project.

### Macwire

[Macwire](https://github.com/adamw/macwire) is a lightweight dependency injection framework that uses Scala macros.

There is an [Activator](http://www.typesafe.com/activator) project available for Macwire.  You can download it from Activator [directly](http://typesafe.com/activator/template/macwire-activator), or clone it from [https://github.com/adamw/macwire-activator](https://github.com/adamw/macwire-activator).

### Guice

[Guice](https://code.google.com/p/google-guice/) is a lightweight dependency injection framework designed for Java.

There is an [Activator](http://www.typesafe.com/activator) project available for Guice.  You can download it from Activator [directly](http://typesafe.com/activator/template/play-guice), or clone it from [https://github.com/typesafehub/play-guice](https://github.com/typesafehub/play-guice).

### Scaldi

[Scaldi](https://github.com/scaldi/scaldi) is a lightweight Scala dependency injection library.

There is an [Activator](http://www.typesafe.com/activator) template available for Scaldi + Play example application. You can download it from Activator [directly](http://typesafe.com/activator/template/scaldi-play-example), or clone it from [https://github.com/scaldi/scaldi-play-example](https://github.com/scaldi/scaldi-play-example). [This article](http://hacking-scala.tumblr.com/post/51407241538/easy-dependency-injection-in-play-framework-with-scaldi) can also help you to make your first steps with Scaldi and Play.
