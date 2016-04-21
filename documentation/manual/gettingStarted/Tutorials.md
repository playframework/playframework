<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play Tutorials

Play's documentation shows the available features and how to use them, but the documentation will not show how to create an application from start to finish.  This is where tutorials and examples come in.

Tutorials and examples are useful for showing a single application at work, especially when it comes to integrating with other systems such as databases or Javascript frameworks.

## Activator Templates

Many tutorials come in the form of templates, which can be downloaded as projects onto your computer.

A full list of templates can be discovered in the [Activator Web Interface](https://www.lightbend.com/activator/docs) or by typing "activator list-templates" at the command line.

Templates are also published on the Lightbend website.  A full list of both official and community contributed templates for Play can be found [here](https://www.lightbend.com/activator/templates#filter:play).

Finally, the core Play templates are available as git repositories on Github under [https://github.com/playframework/](https://github.com/playframework/) and can be cloned directly from there.

### Creating a Project From A Template

In general, whenever you see a template, you can download the template by using the Github project name.  For example, if you have an example Play project on Github called "some-awesome-play-template", you can download and use the template by typing

```
activator new my-local-project-directory some-awesome-play-template 
```

If you do not have activator installed or would prefer to use git, you can always clone the project the old fashioned way:

```
git clone https://github.com/playframework/some-awesome-play-template my-local-project-directory
```

Creating new projects is covered in more detail in [[Creating a new application|NewApplication]].

### Seed Templates

If you are starting off a new Play project and don't want any extras, you can use the seed templates by typing the following at the command prompt:

``` shell
activator new my-scala-project play-scala 
```

or

``` shell
activator new my-java-project play-java
```

If you want to look at the template code without creating a new project, you can see the templates below:

* [play-scala](https://github.com/playframework/playframework/tree/master/templates/play-scala)
* [play-java](https://github.com/playframework/playframework/tree/master/templates/play-java)

### Database / ORM Access

Play is unopinionated about database access, and integrates with many object relational layers (ORMs).  There is out of the box support for Anorm, EBean, Slick, and JPA, but many customers use NoSQL or REST layers and there are many examples of Play using other ORMs not mentioned here.

#### Slick

[Slick](http://slick.typesafe.com/docs/) is a Functional Relational Mapping (FRM) library for Scala that makes it easy to work with relational databases. It allows you to work with stored data almost as if you were using Scala collections while at the same time giving you full control over when a database access happens and which data is transferred. You can also use SQL directly. Execution of database actions is done asynchronously, making Slick a perfect fit for your reactive applications based on Play and Akka.

* [play-isolated-slick](https://github.com/playframework/play-isolated-slick): This template uses a multi-module that hides Slick 3.x behind an API layer, and does not use Play-Slick integration.  It also contains sbt-flyways and use Slick's code generator to create the Slick binding from SQL tables.
* [play-scala-intro](https://github.com/playframework/playframework/tree/master/templates/play-scala-intro): This template uses [PlaySlick](https://www.playframework.com/documentation/2.5.x/PlaySlick) as part of a single Play project.
* [Computer Database with Play-Slick](https://github.com/playframework/play-slick/tree/master/samples/computer-database): This template uses [PlaySlick](https://www.playframework.com/documentation/2.5.x/PlaySlick).  You will need to clone the `play-slick` project from Github and type `project computer-database-sample` in SBT to get to the sample project.

#### JPA

This is a example template showing Play with Java Persistence API (JPA), using Hibernate Entity Manager.  It is included in the Play project itself.

* [play-java-intro](https://github.com/playframework/playframework/tree/master/templates/play-java-intro)

#### Anorm

This is an example template showing Play with [Anorm](https://github.com/playframework/anorm) using Play's [Anorm Integration](https://www.playframework.com/documentation/latest/ScalaAnorm).  It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding.

* [playframework/play-anorm](https://github.com/playframework/play-anorm)

#### EBean

This is an example template that uses [EBean](https://ebean-orm.github.io/) using Play's [Ebean integration](https://www.playframework.com/documentation/2.5.x/JavaEbean). It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding.

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

This is an example template showing how to use manual compile time dependency injection and manual routing with the [SIRD router](https://www.playframework.com/documentation/2.5.x/ScalaSirdRouter), useful for minimal REST APIs and people used to Spray style routing:

* [playframework/play-scala-compile-di-with-tests](https://github.com/playframework/play-scala-compile-di-with-tests)

#### Macwire Dependency Injection

This is an example template showing compile time dependency injection using [Macwire](https://github.com/adamw/macwire).

* [playframework/play-macwire-di](https://github.com/playframework/play-macwire-di)

## Third Party Tutorials

The Play community also has a number of tutorials that cover aspects of Play than the documentation can, or has a different angle.  This is an incomplete list of several helpful blog posts.

Because some of the blog posts have been written a while ago, this section is organized by Play version.

### 2.5.x

#### Dependency Injection

* [Dependency Injection in Play Framework using Scala](http://www.schibsted.pl/2016/04/dependency-injection-play-framework-scala/) by Krzysztof Pado.

#### Akka Streams

* [Akka Streams integration in Play Framework 2.5](https://loicdescotte.github.io/posts/play25-akka-streams/) by Loïc Descotte.
* [Playing with Akka Streams and Twitter](https://loicdescotte.github.io/posts/play-akka-streams-twitter/) by Loïc Descotte.

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

* [Building a Simple REST API with Scala & Play (PART 1)](http://spr.com/building-a-simple-rest-api-with-scala-play-part-1/)
* [Building a simple REST API with Scala & Play! (PART 2)](http://spr.com/building-a-simple-rest-api-with-scala-play-part-2/)

#### Slick

* [Play framework, Slick and MySQL Tutorial](http://pedrorijo.com/blog/play-slick/) by Pedro Rijo.

#### RethinkDB
* [A classic CRUD application with Play 2.4.x, Scala and RethinkDB](https://rklicksolutions.wordpress.com/2016/02/03/play-2-4-x-rethinkdb-crud-application/) by [Rklick](https://github.com/rklick-solutions)

#### Forms

* [How to add a form to a Play application](https://www.theguardian.com/info/developer-blog/2015/dec/30/how-to-add-a-form-to-a-play-application) by Chris Birchall of the Guardian.

#### EmberJS

* [HTML 5 Device Orientation with play, ember and websockets](http://www.cakesolutions.net/teamblogs/go-reactive-activator-contest-reactive-orientation) by Cake Solutions (with [activator template](https://www.lightbend.com/activator/template/reactive-orientation))

#### AngularJS, RequireJS and sbt-web

Marius Soutier has an excellent series on setting up a Javascript interface using AngularJS with Play and sbt-web.  It was originally written for Play 2.1.x, but has been updated for Play 2.4.x

* [RequireJS Optimization with Play 2.1 and WebJars](http://mariussoutier.com/blog/2013/08/25/requirejs-optimization-play-webjars/)
* [Intro to sbt-web](http://mariussoutier.com/blog/2014/10/20/intro-sbt-web/)
* [Understanding sbt and sbt-web settings](http://mariussoutier.com/blog/2014/12/07/understanding-sbt-sbt-web-settings/)
* [Play Angular Require Seed Updates](http://mariussoutier.com/blog/2015/07/25/play-angular-require-seed-updates/)

#### React JS

* [ReactJS Tutorial with Play, Scala and WebJars](http://ticofab.io/react-js-tutorial-with-play_scala_webjars/) by Fabio Tiriticco.
* [A basic example to render UI using ReactJS with Play 2.4.x, Scala and Anorm](https://blog.knoldus.com/2015/07/19/playing-reactjs/) by Knoldus / [activator template](https://github.com/knoldus/playing-reactjs#master)

### 2.3.x

#### REST APIs

* [Playing with Play Framework 2.3.x: REST, pipelines, and Scala](https://blog.shinetech.com/2015/04/21/playing-with-play-framework-2-3-x-rest-pipelines-and-scala/) by Sampson Oliver.

#### Anorm

Knoldus has a nice series of blog posts on Anorm:

* [Employee-Self-Service – Building Reactive Play application with Anorm SQL data access – (Part-1)](https://blog.knoldus.com/2014/03/24/employee-self-service-building-reactive-play-application-with-anorm-sql-data-access/)
* [Employee-Self-Service – Building Reactive Play application with Anorm SQL data access – (Part-2)](https://blog.knoldus.com/2014/03/31/employee-self-service-2/)
* [Employee-Self-Service: Reactive and Non-Blocking Database Access using Play Framework and Anorm – (Part-3)](https://blog.knoldus.com/2014/04/06/employee-self-service-3/)
* [Employee-Self-Service: Reactive and Non-Blocking Database Access using Play Framework and Anorm – (Part-4)](https://blog.knoldus.com/2014/04/13/employee-self-service-reactive-and-non-blocking-database-access-using-play-framework-and-anorm-part-4/)

#### Forms

* [Example form including multiple checkboxes and selection](https://ics-software-engineering.github.io/play-example-form/) by Philip Johnson.
* [UX-friendly conditional form mapping in Play](http://blog.ntcoding.com/2016/02/play-framework-conditional-form-mappings.html) by the VOA

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
