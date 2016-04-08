<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play Tutorials

Play's documentation shows the available features and how to use them, but the documentation will not show how to create an application from start to finish.  This is where tutorials come in.

Tutorials are useful for showing a single application at work, especially when it comes to integrating with other systems such as databases or Javascript frameworks.

## Activator Templates

Many Activator templates come with comprehensive tutorials that guide you to creating an application using the technologies featured by that template.

A full list of templates can be discovered in the [Activator Web Interface](https://www.lightbend.com/activator/docs).

Additionally, templates are also published on the Lightbend website, a full list of both official and community contributed templates for Play can be found [here](https://www.lightbend.com/activator/templates#filter:play).

Lightbend maintains a number of Activator templates.  These have built-in tutorials that you can see by running the application with `activator ui` and then opening the web interface at [http://127.0.0.1:8888/](http://127.0.0.1:8888/) and clicking on the Tutorial tab.

### Introduction

This is where you should start with Play to see a simple example CRUD application.

* [Play Intro in Scala](https://www.lightbend.com/activator/template/play-scala-intro) with [video](https://youtu.be/eNCerkVyQdc)
* [Play Intro in Java](https://www.lightbend.com/activator/template/play-java-intro) with [video](https://youtu.be/bLrmnjPQsZc)

### Reactive Stocks

Reactive Stocks shows several stock prices displayed on a single page web application.

* [Reactive Stocks in Scala](https://github.com/typesafehub/reactive-stocks#master)
* [Reactive Stocks in Java](https://www.lightbend.com/activator/template/reactive-stocks-java8)

### Reactive Maps

Reactive Maps shows the Lightbend Platform with a series of moving actors updated in real time.

* [Reactive Maps in Scala](https://www.lightbend.com/activator/template/reactive-maps)
* [Reactive Maps in Java](https://www.lightbend.com/activator/template/reactive-maps-java)

### Database

* [Play Java with Spring Data JPA](https://www.lightbend.com/activator/template/play-spring-data-jpa): This is a Play example that uses [Spring Data JPA](https://projects.spring.io/spring-data-jpa/).
* [Play Scala with Slick](https://www.lightbend.com/activator/template/play-slick): This template combines Play Framework with [Slick](http://slick.typesafe.com/).
* [Play Scala with Isolated Slick](https://github.com/wsargent/play-slick-3.0): This template creates module that hides Slick behind a DAO object.
* [Play Java with Ebean](https://github.com/typesafehub/activator-computer-database-java): This is a Play example that uses [EBean](https://ebean-orm.github.io/).
* [Play Scala with Anorm](https://github.com/typesafehub/activator-computer-database-scala): This is a Play example that uses [Anorm](https://github.com/playframework/anorm).

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
* [A basic example to render UI using ReactJS with Play 2.4.x, Scala and Anorm](http://blog.knoldus.com/2015/07/19/playing-reactjs/) by Knoldus /
[activator template](https://github.com/knoldus/playing-reactjs#master)

### 2.3.x

#### REST APIs

* [Playing with Play Framework 2.3.x: REST, pipelines, and Scala](http://blog.shinetech.com/2015/04/21/playing-with-play-framework-2-3-x-rest-pipelines-and-scala/) by Sampson Oliver.

#### Anorm

Knoldus has a nice series of blog posts on Anorm:

* [Employee-Self-Service – Building Reactive Play application with Anorm SQL data access – (Part-1)](http://blog.knoldus.com/2014/03/24/employee-self-service-building-reactive-play-application-with-anorm-sql-data-access/)
* [Employee-Self-Service – Building Reactive Play application with Anorm SQL data access – (Part-2)](http://blog.knoldus.com/2014/03/31/employee-self-service-2/)
* [Employee-Self-Service: Reactive and Non-Blocking Database Access using Play Framework and Anorm – (Part-3)](http://blog.knoldus.com/2014/04/06/employee-self-service-3/)
* [Employee-Self-Service: Reactive and Non-Blocking Database Access using Play Framework and Anorm – (Part-4)](http://blog.knoldus.com/2014/04/13/employee-self-service-reactive-and-non-blocking-database-access-using-play-framework-and-anorm-part-4/)

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
