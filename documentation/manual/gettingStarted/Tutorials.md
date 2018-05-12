<!--- Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com> -->
# Play Tutorials

Play's documentation shows the available features and how to use them, but the documentation will not show how to create an application from start to finish.  This is where tutorials and examples come in.

Tutorials and examples are useful for showing a single application at work, especially when it comes to integrating with other systems such as databases or Javascript frameworks.

## Play Maintained Seeds and Example Templates

This section covers the core tutorials and examples from Play.  These are maintained by the core Play team, and so will be based on the latest Play release.

**All of the following projects can be downloaded as example projects from the [download page](https://playframework.com/download).**

### Play Seeds

There are two Play Seeds that are designed expressly for getting started with new Play applications.  They contain a hello world controller and view template, filters, and nothing else.

If you have [sbt 0.13.13 or higher](https://scala-sbt.org) installed, you can create your own Play project using `sbt new`
 using a minimal [`giter8`](http://foundweekends.org/giter8)  template (roughly like a maven archetype).  This is a good choice if you already know Play and want to create a new project immediately.

> **Note**: If running Windows, you may need to run sbt using `sbt.bat` instead of `sbt`. This documentation assumes the command is `sbt`.

#### Java

```bash
sbt new playframework/play-java-seed.g8
```

#### Scala

```bash
sbt new playframework/play-scala-seed.g8
```

### Play Starter Projects

For people using Play for the first time, there is a starter project which introduces Play with some sample controllers and components.

* [play-java](https://github.com/playframework/play-java)
* [play-scala](https://github.com/playframework/play-scala)

or you can download it as an example project from the [download page](https://playframework.com/download).

### Database / ORM Access

Play is non-opinionated about database access, and integrates with many object relational layers (ORMs).  There is out of the box support for Anorm, Ebean, Slick, and JPA, but many customers use NoSQL or REST layers and there are many examples of Play using other ORMs not mentioned here.

#### Slick

[Slick](http://slick.lightbend.com/docs/) is a Functional Relational Mapping (FRM) library for Scala that makes it easy to work with relational databases. It allows you to work with stored data almost as if you were using Scala collections while at the same time giving you full control over when a database access happens and which data is transferred. You can also use SQL directly. Execution of database actions is done asynchronously, making Slick a perfect fit for your reactive applications based on Play and Akka.

* [play-isolated-slick](https://github.com/playframework/play-isolated-slick): This template uses a multi-module that hides Slick 3.x behind an API layer, and does not use Play-Slick integration.  It also contains sbt-flyways and use Slick's code generator to create the Slick binding from SQL tables.
* [play-scala-intro](https://github.com/playframework/play-scala-intro): This template uses [Play Slick](https://www.playframework.com/documentation/%PLAY_VERSION%/PlaySlick) as part of a single Play project.
* [Computer Database with Play-Slick](https://github.com/playframework/play-slick/tree/master/samples/computer-database): This template uses [Play Slick](https://www.playframework.com/documentation/%PLAY_VERSION%/PlaySlick).  You will need to clone the `play-slick` project from Github and type `project computer-database-sample` in SBT to get to the sample project.

#### JPA

This is a example template showing Play with Java Persistence API (JPA), using Hibernate Entity Manager.  It is included in the Play project itself.

* [play-java-intro](https://github.com/playframework/play-java-intro)

#### Anorm

This is an example template showing Play with [Anorm](https://github.com/playframework/anorm) using Play's [Anorm Integration](https://www.playframework.com/documentation/latest/ScalaAnorm).  It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding.

* [playframework/play-anorm](https://github.com/playframework/play-anorm)

#### Ebean

This is an example template that uses [Ebean](https://ebean-orm.github.io/) using Play's [Ebean integration](https://www.playframework.com/documentation/%PLAY_VERSION%/JavaEbean). It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding.

* [playframework/play-ebean-example](https://github.com/playframework/play-ebean-example)

### Comet / Server Sent Events (SSE)

This is an example template that shows streaming responses through Comet or Server Sent Events, using Akka Streams:

* [playframework/play-streaming-scala](https://github.com/playframework/play-streaming-scala)
* [playframework/play-streaming-java](https://github.com/playframework/play-streaming-java)

### WebSocket

This is an example template that shows bidirectional streaming through the WebSocket API, using Akka Streams:

* [playframework/play-websocket-scala](https://github.com/playframework/play-websocket-scala)
* [playframework/play-websocket-java](https://github.com/playframework/play-websocket-java)

### Cryptography

This is an example template showing how to encrypt and sign data securely with [Kalium](https://github.com/abstractj/kalium):

* [playframework/play-kalium](https://github.com/playframework/play-kalium)

### Compile Time Dependency Injection

[[Compile time dependency injection|ScalaCompileTimeDependencyInjection]] can be done in Play in a number of different DI frameworks.

There are two examples shown here, but there are other compile time DI frameworks such as Scaldi, which has [Play integration](http://scaldi.org/learn/#play-integration) built in, and [Dagger 2](https://google.github.io/dagger/), which is written in Java.

#### Manual Compile Time Dependency Injection

This is an example template showing how to use manual compile time dependency injection and manual routing with the [[SIRD router|ScalaSirdRouter]], useful for minimal REST APIs and people used to Spray style routing:

* [playframework/play-scala-compile-di-with-tests](https://github.com/playframework/play-scala-compile-di-with-tests)

#### Macwire Dependency Injection

This is an example template showing compile time dependency injection using [Macwire](https://github.com/adamw/macwire).

* [playframework/play-macwire-di](https://github.com/playframework/play-macwire-di)

## Third Party Tutorials and Templates

The Play community also has a number of tutorials and templates that cover aspects of Play than the documentation can, or has a different angle.  Templates listed here are not maintained by the Play team, and so may be out of date.

This is an incomplete list of several helpful blog posts, and because some of the blog posts have been written a while ago, this section is organized by Play version.

### 2.5.x

#### Play Framework Tutorial Video Series

A tutorial video series by Radix Code provides an initial overview to Play, walking through initial IDE setup, defining routes, creating a CRUD application, enabling ORM support, and customizing the views with bootstrap.

* [Debug Play Application in IntelliJ IDE](https://www.youtube.com/watch?v=RVKU9JvZmao)
* [Debug Play Application in Eclipse IDE](https://www.youtube.com/watch?v=f9TQD_V7rLg)
* [How Routing Works](https://www.youtube.com/watch?v=SnQQYl4xsN8)
* [Add Support for MySQL in Play](https://www.youtube.com/watch?v=J22fr8gQn2c)
* [Include Bootstrap and jQuery](https://www.youtube.com/watch?v=XyoZnTBUM5I)
* [Form Validations](https://www.youtube.com/watch?v=Wec-mbjQsrk)
* [Creating Custom Error Pages](https://www.youtube.com/watch?v=nhKpMrT2EZA)

#### Dependency Injection

* [Dependency Injection in Play Framework using Scala](https://www.schibsted.pl/blog/dependency-injection-play-framework-scala/) by Krzysztof Pado.

#### Akka Streams

* [Akka Streams integration in Play Framework 2.5](https://loicdescotte.github.io/posts/play25-akka-streams/) by Loïc Descotte.
* [Playing with Akka Streams and Twitter](https://loicdescotte.github.io/posts/play-akka-streams-twitter/) by Loïc Descotte.

#### Database

* [Play Database Application using Slick, Bootstrap](https://www.lightbend.com/activator/template/activator-play-slick-app): This is an example project for showcasing best practices and providing a seed for starting with Play &amp; Slick, By [Knoldus](http://www.knoldus.com/home.knol).

#### Forms and Validators

* [Controller Forms](http://queirozf.com/entries/play2-scala-forms-and-validations): This provides examples of using forms and custom validators within a controller.
* [Json Validators](http://queirozf.com/entries/fully-customized-json-validator-for-play-framework-2): This guide lists methods of validating json against a customized case class or trait.

#### REST APIs

* [Making a REST API in Play](https://github.com/playframework/play-rest-api), a multi-part guide using the Scala API, by the Lightbend Play Team.
* [Play API REST Template](https://github.com/adrianhurt/play-api-rest-seed) by Adrianhurt: shows how to implement a complete Json RESTful API with some characteristics such as Authentication Token, pagination, filtering, sorting and searching and optional enveloping.

#### Sub-projects

* [Play Multidomain Seed](https://github.com/adrianhurt/play-multidomain-seed) by Adrianhurt: tries to be a skeleton for a simple multidomain project (www.myweb.com and admin.myweb.com). It shows you how to use subprojects for that and how to share common code. It is also ready to use with Webjars, CoffeeScript, LESS, RequireJS, assets Gzip and assets fingerprinting. Please, check the readme file for more details.
* [Play Multidomain Auth](https://github.com/adrianhurt/play-multidomain-auth) by Adrianhurt: this is a second part of play-multidomain-seed project. This project tries to be an example of how to implement an Authentication and Authorization layer using the Silhouette authentication library. It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding.

#### Upgrading

* [Upgrading from Play 2.3 to Play 2.5](https://www.lucidchart.com/techblog/2017/02/22/upgrading-play-framework-2-3-play-2-5/) by Gregg Hernandez: Learn how to deal with common problems when upgrading to Play 2.5, including maintaining legacy behavior, transitioning to Akka Streams, and implementing compile-time dependency injection.

### 2.4.x

#### Semisafe

Semisafe has an excellent series on Play in general:

* [Templates, Routes and AJAX](http://semisafe.com/coding/2015/03/31/play_basics_templates_and_ajax.html)
* [Upgrading the Framework](http://semisafe.com/coding/2015/06/01/play_basics_upgrading_the_framework.html)
* [Database Access](http://semisafe.com/coding/2015/06/12/play_basics_database_access.html)
* [Async Futures and Actors](http://semisafe.com/coding/2015/06/22/play_basics_async_futures_and_actors.html)
* [Optimistic Future Composition](http://semisafe.com/coding/2015/07/14/play_basics_optimistic_future_composition.html)
* [React UI Coffeescript](http://semisafe.com/coding/2015/07/24/play_basics_ui_react_coffeescript.html)
* [CSRF Protection](http://semisafe.com/coding/2015/08/03/play_basics_csrf_protection.html)

#### Minimal Play

* [A Play Application in 38 Lines](https://beachape.com/blog/2015/07/25/slim-play-app/) by Lloyd Chan, showing a "Sinatra" style of Play application.

#### Dependency Injection

* [Playframework 2.4 Dependency Injection (DI)](http://mariussoutier.com/blog/2015/12/06/playframework-2-4-dependency-injection-di/) by Marius Soutier.
* [Testing with Dependency Injection](http://www.michaelpollmeier.com/2015/09/25/playframework-guice-di) by Michael Pollmeier.
* [Compile Time Dependency Injection with Play 2.4](https://loicdescotte.github.io/posts/play24-compile-time-di/) by Loïc Descotte.

#### REST APIs

Justin Rodenbostel of SPR Consulting also has two blog posts on building REST APIs in Play:

* [Building a Simple REST API with Scala & Play! (PART 1)](https://spr.com/building-a-simple-rest-api-with-scala-play-part-1/)
* [Building a Simple REST API with Scala & Play! (PART 2)](https://spr.com/building-a-simple-rest-api-with-scala-play-part-2/)

#### Slick

* [Play framework, Slick and MySQL Tutorial](http://pedrorijo.com/blog/play-slick/) by Pedro Rijo.

#### RethinkDB

* [A classic CRUD application with Play 2.4.x, Scala and RethinkDB](https://rklicksolutions.wordpress.com/2016/02/03/play-2-4-x-rethinkdb-crud-application/) by [Rklick](https://github.com/rklick-solutions).

#### Forms

* [How to add a form to a Play application](https://www.theguardian.com/info/developer-blog/2015/dec/30/how-to-add-a-form-to-a-play-application) by Chris Birchall of the Guardian.

#### EmberJS

* [HTML 5 Device Orientation with play, ember and websockets](https://www.cakesolutions.net/teamblogs/go-reactive-activator-contest-reactive-orientation) by Cake Solutions (with [activator template](https://www.lightbend.com/activator/template/reactive-orientation)).

#### AngularJS, RequireJS and sbt-web

Marius Soutier has an excellent series on setting up a Javascript interface using AngularJS with Play and sbt-web.  It was originally written for Play 2.1.x, but has been updated for Play 2.4.x.

* [RequireJS Optimization with Play 2.1 and WebJars](http://mariussoutier.com/blog/2013/08/25/requirejs-optimization-play-webjars/)
* [Intro to sbt-web](http://mariussoutier.com/blog/2014/10/20/intro-sbt-web/)
* [Understanding sbt and sbt-web settings](http://mariussoutier.com/blog/2014/12/07/understanding-sbt-sbt-web-settings/)
* [Play Angular Require Seed Updates](http://mariussoutier.com/blog/2015/07/25/play-angular-require-seed-updates/)

#### React JS

* [ReactJS Tutorial with Play, Scala and WebJars](http://ticofab.io/react-js-tutorial-with-play_scala_webjars/) by Fabio Tiriticco.
* [A basic example to render UI using ReactJS with Play 2.4.x, Scala and Anorm](https://blog.knoldus.com/2015/07/19/playing-reactjs/) by Knoldus / [activator template](https://github.com/knoldus/playing-reactjs#master).

### 2.3.x

#### REST APIs

* [Playing with Play Framework 2.3.x: REST, pipelines, and Scala](https://shinesolutions.com/2015/04/21/playing-with-play-framework-2-3-x-rest-pipelines-and-scala/) by Sampson Oliver.

#### Anorm

Knoldus has a nice series of blog posts on Anorm:

* [Employee-Self-Service – Building Reactive Play application with Anorm SQL data access – (Part-1)](https://blog.knoldus.com/2014/03/24/employee-self-service-building-reactive-play-application-with-anorm-sql-data-access/)
* [Employee-Self-Service – Building Reactive Play application with Anorm SQL data access – (Part-2)](https://blog.knoldus.com/2014/03/31/employee-self-service-2/)
* [Employee-Self-Service: Reactive and Non-Blocking Database Access using Play Framework and Anorm – (Part-3)](https://blog.knoldus.com/2014/04/06/employee-self-service-3/)
* [Employee-Self-Service: Reactive and Non-Blocking Database Access using Play Framework and Anorm – (Part-4)](https://blog.knoldus.com/2014/04/13/employee-self-service-reactive-and-non-blocking-database-access-using-play-framework-and-anorm-part-4/)

#### Forms

* [Example form including multiple checkboxes and selection](https://ics-software-engineering.github.io/play-example-form/) by Philip Johnson.
* [UX-friendly conditional form mapping in Play](http://ntcoding.com/blog/2016/02/play-framework-conditional-form-mappings) by Nick Tune.

### 2.2.x

#### Advanced Routing

* [Advanced routing in Play Framework](https://jazzy.id.au/2013/05/08/advanced_routing_in_play_framework.html) by James Roper.
* [Play Routes – Part 1, Basics](http://mariussoutier.com/blog/2012/12/10/playframework-routes-part-1-basics/) by Marius Soutier.
* [Play Routes – Part 2, Advanced Use Cases](http://mariussoutier.com/blog/2012/12/11/playframework-routes-part-2-advanced/) by Marius Soutier.

#### Path Bindables

* [How to implement a custom PathBindable with Play 2](http://julien.richard-foy.fr/blog/2012/04/09/how-to-implement-a-custom-pathbindable-with-play-2/) by Julien Richard-Foy.

#### Templates

* [Play Framework 2.0 Templates – Part 1, Parameters](http://mariussoutier.com/blog/2012/04/27/play-framework-2-0-templates-part-1-parameters/) by Marius Soutier.

#### User Interface

* [Composite user interface without boilerplate using Play 2](http://julien.richard-foy.fr/blog/2012/02/26/composite-user-interface-without-boilerplate-using-play-2/) by Julien Foy.

#### Play in Practice

* [Play in Practice](https://tersesystems.com/2013/04/20/play-in-practice/) by Will Sargent.
