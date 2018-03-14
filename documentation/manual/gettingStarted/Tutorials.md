<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Other resources

Play's documentation shows the available features and how to use them, but does not show how to create an application from start to finish.  This is where [[tutorials and examples|NewApplication]] come in.

<!-- Tutorials and examples are useful for showing a single application at work, especially when it comes to integrating with other systems such as databases or Javascript frameworks.

## Play Maintained Seeds and Example Templates

This section covers the core tutorials and examples from Play.  These are maintained by the core Play team, and so will be based on the latest Play release.

**All of the following projects can be downloaded as example projects from the [download page](https://playframework.com/download).**
-->
<!-- ### Play Seeds

There are two Play Seeds that are designed expressly for getting started with new Play applications.  They contain a hello world controller and view template, filters, and nothing else.

If you have [sbt 0.13.13 or higher](http://scala-sbt.org) installed, you can create your own Play project using `sbt new`
 using a minimal [`giter8`](http://foundweekends.org/giter8)  template (roughly like a maven archetype).  This is a good choice if you already know Play and want to create a new project immediately.

> **Note**: If running Windows, you may need to run sbt using `sbt.bat` instead of `sbt`. This documentation assumes the command is `sbt`.

Type `g8Scaffold form` from sbt to create the scaffold controller, template and tests needed to process a form.

#### Java

```
sbt new playframework/play-java-seed.g8
```

#### Scala

```
sbt new playframework/play-scala-seed.g8
```

### Play Starter Projects

For people using Play for the first time, there is a starter project which introduces Play with some sample controllers and components.

* [play-java](https://github.com/playframework/play-java)
* [play-scala](https://github.com/playframework/play-scala)

or you can download it as an example project from the [download page](https://playframework.com/download).





## Third Party Tutorials and Templates -->

The Play community has developed videos, blogs, tutorials and templates that cover aspects of Play from a different angle than the documentation.  Templates listed here are not maintained by the Play team, and so may be out of date.

We recommend that you develop new apps with the latest release of Play. The resources below were created for previous versions, but still contain useful information. The sections group them by Play version: 

* [2.5.x](#2.5.x)
* [2.4.x](#2.4.x)
* [2.3.x](#2.3.x)
* [2.2.x](#2.2.x)

### 2.5.x
The following resources were created for Play version 2.5.x:

* A tutorial video series by Radix Code provides an initial overview to Play:

    * [Debug Play Application in IntelliJ IDE](https://www.youtube.com/watch?v=RVKU9JvZmao)
    * [Debug Play Application in Eclipse IDE](https://www.youtube.com/watch?v=f9TQD_V7rLg)
    * [How Routing Works](https://www.youtube.com/watch?v=SnQQYl4xsN8)
    * [Add Support for MySQL in Play](https://www.youtube.com/watch?v=J22fr8gQn2c)
    * [Include Bootstrap and jQuery](https://www.youtube.com/watch?v=XyoZnTBUM5I)
    * [Form Validations](https://www.youtube.com/watch?v=Wec-mbjQsrk)
    * [Creating Custom Error Pages](https://www.youtube.com/watch?v=nhKpMrT2EZA)

* [Dependency Injection in Play Framework using Scala](http://www.schibsted.pl/2016/04/dependency-injection-play-framework-scala/) by Krzysztof Pado.

* Loïc Descotte offers two posts on using Play with Akka Streams: [Akka Streams integration in Play Framework 2.5](https://loicdescotte.github.io/posts/play25-akka-streams/) and [Playing with Akka Streams and Twitter](https://loicdescotte.github.io/posts/play-akka-streams-twitter/).

* [Play Database Application using Slick, Bootstrap](https://www.lightbend.com/activator/template/activator-play-slick-app) showcases best practices and provides a seed for starting with Play &amp; Slick, by [Knoldus](http://www.knoldus.com/home.knol).

* [Controller Forms](http://queirozf.com/entries/play2-scala-forms-and-validations): examples of using forms and custom validators within a controller.
    
* [Json Validators](http://queirozf.com/entries/fully-customized-json-validator-for-play-framework-2):  lists methods of validating json against a customized case class or trait.

* [Making a REST API in Play](https://github.com/playframework/play-rest-api), a multi-part guide using the Scala API, by the Lightbend Play Team.
    
* [Play API REST Template](https://github.com/adrianhurt/play-api-rest-seed) by Adrianhurt: shows how to implement a complete Json RESTful API with some characteristics such as Authentication Token, pagination, filtering, sorting and searching and optional enveloping.

* [Play Multidomain Seed](https://github.com/adrianhurt/play-multidomain-seed) by Adrianhurt offers a skeleton for a simple multidomain project (www.myweb.com and admin.myweb.com). It shows you how to use subprojects and how to share common code. It is also ready to use with Webjars, CoffeeScript, LESS, RequireJS, assets Gzip and assets fingerprinting. Check the readme file for more details.
    
* [Play Multidomain Auth](https://github.com/adrianhurt/play-multidomain-auth) by Adrianhurt is the second part of play-multidomain-seed project. This project is an example of how to implement an Authentication and Authorization layer using the Silhouette authentication library. It also uses [Play-Bootstrap](https://adrianhurt.github.io/play-bootstrap/) for easy template scaffolding.

* [Upgrading from Play 2.3 to Play 2.5](https://www.lucidchart.com/techblog/2017/02/22/upgrading-play-framework-2-3-play-2-5/) by Gregg Hernandez shows how to deal with common problems when upgrading to Play 2.5, including maintaining legacy behavior, transitioning to Akka Streams, and implementing compile-time dependency injection.

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

* [Building a Simple REST API with Scala & Play! (PART 1)](http://spr.com/building-a-simple-rest-api-with-scala-play-part-1/)
* [Building a Simple REST API with Scala & Play! (PART 2)](http://spr.com/building-a-simple-rest-api-with-scala-play-part-2/)

#### Slick

* [Play framework, Slick and MySQL Tutorial](http://pedrorijo.com/blog/play-slick/) by Pedro Rijo.

#### RethinkDB

* [A classic CRUD application with Play 2.4.x, Scala and RethinkDB](https://rklicksolutions.wordpress.com/2016/02/03/play-2-4-x-rethinkdb-crud-application/) by [Rklick](https://github.com/rklick-solutions).

#### Forms

* [How to add a form to a Play application](https://www.theguardian.com/info/developer-blog/2015/dec/30/how-to-add-a-form-to-a-play-application) by Chris Birchall of the Guardian.

#### EmberJS

* [HTML 5 Device Orientation with play, ember and websockets](http://www.cakesolutions.net/teamblogs/go-reactive-activator-contest-reactive-orientation) by Cake Solutions (with [activator template](https://www.lightbend.com/activator/template/reactive-orientation)).

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
