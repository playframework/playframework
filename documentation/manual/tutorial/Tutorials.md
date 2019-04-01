<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Play Tutorials

Play's documentation shows the available features and how to use them, but the documentation will not show how to create an application from start to finish.  This is where tutorials and examples come in.

Tutorials and examples are useful for showing a single application at work, especially when it comes to integrating with other systems such as databases or Javascript frameworks.

The Play team uses [Lightbend Tech Hub](https://developer.lightbend.com/start/?group=play) to publish tutorials that cover a huge number of cases. There you can find projects in Java, Scala and for multiple versions of Play. You can pick one that demonstrates functionality of interest to you. The examples you can download cover the following topics:

### Java

| Example                                   | Repository                                                                         |
|:------------------------------------------|:-----------------------------------------------------------------------------------|
| REST API Example                          | [GitHub](https://github.com/playframework/play-java-rest-api-example/tree/2.6.x)   |
| File Upload Example                       | [GitHub](https://github.com/playframework/play-java-fileupload-example/tree/2.6.x) |
| Forms Example                             | [GitHub](https://github.com/playframework/play-java-forms-example/tree/2.6.x)      |
| JPA Example                               | [GitHub](https://github.com/playframework/play-java-jpa-example/tree/2.6.x)        |
| Ebean Example                             | [GitHub](https://github.com/playframework/play-java-ebean-example/tree/2.6.x)      |
| Websocket Example                         | [GitHub](https://github.com/playframework/play-java-websocket-example/tree/2.6.x)  |
| Chatroom using Websockets Example         | [GitHub](https://github.com/playframework/play-java-chatroom-example/tree/2.6.x)   |
| Streaming Example                         | [GitHub](https://github.com/playframework/play-java-streaming-example/tree/2.6.x)  |
| Compile Time Dependency Injection Example | [GitHub](https://github.com/playframework/play-java-compile-di-example/tree/2.6.x) |
| Using Dagger 2 for Compile Time DI        | [GitHub](https://github.com/playframework/play-java-dagger2-example/tree/2.6.x)    |

### Scala

| Example                                    | Repository                                                                                  |
|:-------------------------------------------|:--------------------------------------------------------------------------------------------|
| REST API Example                           | [GitHub](https://github.com/playframework/play-scala-rest-api-example/tree/2.6.x)           |
| File Upload Example                        | [GitHub](https://github.com/playframework/play-scala-fileupload-example/tree/2.6.x)         |
| Forms Example                              | [GitHub](https://github.com/playframework/play-scala-forms-example/tree/2.6.x)              |
| Anorm Example                              | [GitHub](https://github.com/playframework/play-scala-anorm-example/tree/2.6.x)              |
| Integrated Slick Example                   | [GitHub](https://github.com/playframework/play-scala-slick-example/tree/2.6.x)              |
| Isolated Slick Example                     | [GitHub](https://github.com/playframework/play-scala-isolated-slick-example/tree/2.6.x)     |
| Websocket Example                          | [GitHub](https://github.com/playframework/play-scala-websocket-example/tree/2.6.x)          |
| Chatroom using Websockets Example          | [GitHub](https://github.com/playframework/play-scala-chatroom-example/tree/2.6.x)           |
| Streaming Example                          | [GitHub](https://github.com/playframework/play-scala-streaming-example/tree/2.6.x)          |
| Compile Time Dependency Injection Example  | [GitHub](https://github.com/playframework/play-scala-compile-di-example/tree/2.6.x)         |
| Dependency Injection using Macwire Example | [GitHub](https://github.com/playframework/play-scala-macwire-di-example/tree/2.6.x)         |
| Secure Session Example                     | [GitHub](https://github.com/playframework/play-scala-secure-session-example/tree/2.6.x)     |

## Third Party Tutorials and Templates

The Play community also has a number of tutorials and templates that cover aspects of Play than the documentation can, or has a different angle.  Templates listed here are not maintained by the Play team, and so may be out of date.

This is an incomplete list of several helpful blog posts, and because some of the blog posts have been written a while ago, this section is organized by Play version.

### 2.6.x

#### Play Framework Tutorials and other contents

* [Running Play on GraalVM](https://blog.playframework.com/play-on-graal/): Play's core contributor Christian Schmitt explains how to run Play applications using [GraalVM](https://www.graalvm.org/) and the challenges and benefits of using GraalVM with Play.
* [Getting Started With Play Framework](https://dzone.com/refcardz/getting-started-play-framework): This DZone's reference card shows the most basic concepts of Play in a resumed but very informative way.
* [Play: The Missing Tutorial](https://github.com/shekhargulati/play-the-missing-tutorial/blob/master/01-hello-world.md): In this tutorial series, Shekhar Gulati
 shows how to build a blogging platform called blogy that you can use to write and publish blogs.
* [Our adventure using Play Framework with Kotlin](https://blog.karumi.com/our-adventure-using-play-framework-in-kotlin/): This article written by [Antonio López Marín](http://tonilopezmr.github.io/) for [Karumi](https://www.karumi.com/) details the steps necessary to write a Play application using Kotlin language.
* [Add Authentication to Play Framework with OIDC and Okta](https://developer.okta.com/blog/2017/10/31/add-authentication-to-play-framework-with-oidc): [Matt Raible](https://twitter.com/mraible) shows how easy it is to integrate Play with a modern authentication mechanism like OpenID Connect using [play-pac4j](https://github.com/pac4j/play-pac4j).
* [REST API using Play Framework with Java](http://softwaredevelopercentral.blogspot.com/2017/10/rest-api-using-play-framework-with-java.html): This article shows how to create an application using Play Framework and Java with `GET`, `POST`, `PUT` and `DELETE` APIs for CRUD operations.
* [RESTful APIs With the Play Framework - Part 1](https://dzone.com/articles/restful-apis-with-play-framework-part-1) & [RESTful APIs With the Play Framework — Part 2](https://dzone.com/articles/restful-apis-with-play-frameworkpartnbsp2): In this two part tutorial, [Mercedes Wyss](https://twitter.com/itrjwyss) gives a look into how to set up your development environment using the Play framework, and how to get Play going on your machine, and later at creating RESTful APIs exploring how to handle JSON in your code.
* [Creating forms on your Play application - Part 1](https://pedrorijo.com/blog/play-forms/) & [Creating forms on your Play application - Part 2](https://pedrorijo.com/blog/advanced-play-forms/): Pedro Rijo goes from basic to advanced examples showing the helpers that Play provides when dealing with HTML forms, how to validate some inputs, and how does Play deals with those input errors.
* [React with Play Framework 2.6.x](https://medium.com/@yohan.gz/react-with-play-framework-2-6-x-a6e15c0b7bd): Yohan Gomez explains the pros and cons of different approaches when integrating React and Play, and later how to structure your project when using both. There are seed projects for both Java and Scala.
* [Angular 6 with Play Framework 2.6.x](https://medium.com/@yohan.gz/https-medium-com-yohan-gz-angular-with-play-framework-a6c3f8b339f3): Again Yohan Gomez explains how to integrate Play and modern frontend frameworks, but this time with Angular 6. There are seed projects for both Java and Scala.
* [Internationalization with Play Framework](https://blog.knoldus.com/internationalization-with-play-framework2-6-x/): Teena Vashist demonstrate how your application can support different languages using Play Framework 2.6.
* [Authentication using Actions in Play Framework](https://blog.knoldus.com/authentication-using-actions-in-play-framework/): Geetika Gupta demonstrates how to use Action Composition to handle authentication in Play applications.
* [Streaming data from PostgreSQL using Akka Streams and Slick in Play Framework](https://blog.knoldus.com/streaming-data-from-postgresql-using-akka-streams-and-slick-in-play-framework/): In this blog post, Sidharth Khattri explains the process wherein you can stream data directly from PostgreSQL database using Scala Slick (which is Scala’s database access/query library) and Akka Streams.
* [Stream a file to AWS S3 using Akka Streams (via Alpakka) in Play Framework](https://blog.knoldus.com/stream-a-file-to-aws-s3-using-akka-streams-via-alpakka-in-play-framework/): In this blog post Sidharth Khattri explains how a file can be streamed from a client (eg: browser) to Amazon S3 using [Alpakka's](https://developer.lightbend.com/docs/alpakka/current/) AWS [S3 connector](https://developer.lightbend.com/docs/alpakka/current/s3.html).

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

* [Play Database Application using Slick, Bootstrap](https://www.lightbend.com/activator/template/activator-play-slick-app): This is an example project for showcasing best practices and providing a seed for starting with Play &amp; Slick, By [Knoldus](https://www.knoldus.com/home.knol).

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

* [Play framework, Slick and MySQL Tutorial](https://pedrorijo.com/blog/play-slick/) by Pedro Rijo.

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
