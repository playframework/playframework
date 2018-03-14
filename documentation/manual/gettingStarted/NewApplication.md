<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Examples and templates

Play provides example projects and templates to help you get started:

* To learn more about Play, try an [example project](#Using-Play-examples). Each example showcases a particular feature or illustrates how to satisfy a common use case.
* When you are ready to build your own app, start with a [template](#Using-templates). Templates set up the appropriate project structure and dev environment for you. 

**Note**: Templates are already configured with [[CSRF|ScalaCsrf]] and [[security headers filters|SecurityHeaders]], whereas example projects are not set up for security out-of-the-box.

This page provides information on:

* [Using Play examples](#Using-Play-examples)
* [Using templates](#Using-templates)
* [Examples by category](#Examples-by-category)

## Using Play examples

Play examples are available from Lightbend [Tech Hub](https://developer.lightbend.com/start/?group=play). The [Overview of available examples](#Overview-of-available-examples) section below briefly describes them by category to help you choose. For your first experience with Play, we suggest the Play Starter Project for Java or Scala.   

After choosing an example from [Tech Hub](https://developer.lightbend.com/start/?group=play), follow these steps:

1. Click CREATE A PROJECT FOR ME to download the zipped project.
1. Unzip the project in a convenient location.
1. In a command window, navigate to the top level project directory.
1. Enter one of the following commands:
   On OSx or Linux systems: `/sbt run` or `./gradle runPlayBinary`
   On Windows systems: `sbt run` or `gradle runPlayBinary`
  
   The build tool downloads dependencies and and starts the HTTP server. When it finishes, enter the following URL in a browser to view the app:
   
   `http://localhost:9000`
  
  The request causes `sbt` to compile the application and if successful, the index page will display in your browser.
  
## Using templates

If you have [sbt 0.13.13 or higher](http://www.scala-sbt.org) installed, you can create a Play project using a minimal [giter8](http://foundweekends.org/giter8) seed template (similar to a maven archetype). This is a good choice if you already know Play and want to create a new project immediately. You can also create your own giter8 templates by forking from the https://github.com/playframework/play-java-seed.g8 or https://github.com/playframework/play-scala-seed.g8 github projects.

In a command window, enter one of the following lines to create a project using a seed template:

### Play Java Seed

```bash
sbt new playframework/play-java-seed.g8
```

### Play Scala Seed

```bash
sbt new playframework/play-scala-seed.g8
```

If you plan to use forms in your application, enter `sbt g8Scaffold form` to create the scaffold controller, template and tests needed to process a form.

After that, use `sbt run` and enter http://localhost:9000 in a browser. 

## Examples by category

Both Lightbend [Tech Hub](https://developer.lightbend.com/start/?group=play) and github offer a variety of Play examples. Some are available for different versions of Play. The following sections organize these examples by category, describe them briefly, and provide links to Tech Hub, where you can download them as zip files:

* [ORM layers](#ORM-layers)
* [Streaming and event-driven](#Streaming-and-event-driven)
* [Security and cryptography](#Security-and-cryptography)
* [Dependency injection](#Dependency-injection)

### ORM layers 

Play examples demonstrate how to use the folloing ORMs:

* [Slick](http://slick.lightbend.com/docs/) is a Functional Relational Mapping (FRM) library for Scala that makes it easy to work with relational databases. It allows you to work with stored data almost as if you were using Scala collections while at the same time giving you full control over database access and data transfer. You can also use SQL directly. Database actions execute asynchronously, making Slick a perfect fit for your reactive applications based on Play and Akka. The following Play examples demonstrate use of Slick:
    * [play-isolated-slick](https://developer.lightbend.com/start/?group=play&project=play-scala-isolated-slick-example-2.5.x): This project uses a multi-module that hides Slick 3.x behind an API layer, and does not use Play-Slick integration.  It also contains sbt-flyways and use Slick's code generator to create the Slick binding from SQL tables.
    
    * [Computer Database with Play-Slick](https://github.com/playframework/play-slick/tree/master/samples/computer-database): This template uses [Play Slick](https://www.playframework.com/documentation/%PLAY_VERSION%/PlaySlick).  You will need to clone the `play-slick` project from Github and type `project computer-database-sample` in SBT to get to the sample project.

* [play-scala-intro](https://developer.lightbend.com/start/?group=play&project=play-2.4.x-scala-intro)  demonstrates how to create a simple CRUD application.

* [play-java-intro](https://developer.lightbend.com/start/?group=play&project=play-2.4.x-java-intro) shows how to use Play with Java Persistence API (JPA), using Hibernate Entity Manager.  

* [playframework/play-anorm](https://developer.lightbend.com/start/?group=play&project=play-scala-anorm-example) showing Play with [Anorm](https://github.com/playframework/anorm) using Play's [Anorm Integration](https://www.playframework.com/documentation/latest/ScalaAnorm).  It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding. 


* [playframework/play-ebean-example](https://developer.lightbend.com/start/?group=play&project=play-java-ebean-example) uses [Ebean](https://ebean-orm.github.io/) using Play's [Ebean integration](https://www.playframework.com/documentation/%PLAY_VERSION%/JavaEbean). It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding. 

### Streaming and event-driven

* This streaming example uses Akka Streams and responses through Comet or Server Sent Events (SSE):

    * [playframework/play-streaming-scala](https://developer.lightbend.com/start/?group=play&project=play-scala-streaming-example)
    * [playframework/play-streaming-java](https://developer.lightbend.com/start/?group=play&project=play-java-streaming-example)


* This chatroom example shows bidirectional streaming through the WebSocket API, using Akka Streams:
    * [playframework/play-websocket-scala](https://developer.lightbend.com/start/?group=play&project=play-scala-chatroom-example)
    * [playframework/play-websocket-java](https://developer.lightbend.com/start/?group=play&project=play-java-chatroom-example)

* [Reactive Maps](https://developer.lightbend.com/start/?group=play&project=reactive-maps) shows how to implement scalable, resilient, and responsive event-driven apps. It includes an in-context tutorial.

### Security and cryptography

*  [Play Secure HTTP(SSL/TLS) Example](https://developer.lightbend.com/start/?group=play&project=play-scala-tls-example) uses the Java Secure Socket Extension API

* Play Scala Secure Session Example shows how to encrypt and sign data securely with [Kalium](https://github.com/abstractj/kalium): [playframework/play-kalium](https://developer.lightbend.com/start/?group=play&project=play-scala-secure-session-example)

### Dependency injection

[[Compile time dependency injection|ScalaCompileTimeDependencyInjection]] can be added to Play using a number of different DI frameworks. There are two examples shown here, but other compile time DI frameworks exist, such as Scaldi, which has [Play integration](http://scaldi.org/learn/#play-integration) built in, and [Dagger 2](https://google.github.io/dagger/), which is written in Java.

* [Compile DI](https://developer.lightbend.com/start/?group=play&project=play-scala-compile-di-example) shows how to use manual compile time dependency injection and manual routing with the [[SIRD router|ScalaSirdRouter]], useful for minimal REST APIs and people used to Spray style routing: 


* [Play Macwire DI](https://developer.lightbend.com/start/?group=play&project=play-scala-macwire-di-example) example template demonstrates compile time dependency injection using [Macwire](https://github.com/adamw/macwire).  