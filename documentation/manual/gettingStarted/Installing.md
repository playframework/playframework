<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Introduction

In contrast with web frameworks that were designed to support large eco-systems, such as Java EE, Play was developed by web developers&mdash;for web development. Play saves precious development time by directly supporting common tasks and hot reloading so that you can immediately view the results of your work. As a full-stack framework, it includes all of the components you need to build a web application such as an integrated HTTP server, form validation, Cross-Site Request Forgery (CSRF) protection, RESTful web services API, and more.

Play Framework uses Scala and Akka under the hood. This endows Play applications with a stateless, non-blocking, event-driven architecture that provides horizontal and vertical scalability and uses resources more efficiently. See [Elasticity](https://developer.lightbend.com/elastic-scaling/) and [Efficient Resource Usage](https://developer.lightbend.com/efficient-resource-usage/) for more information.

Play offers both Java and Scala APIs. Java developers find Play's Model-View-Controller (MVC) architecture familiar and easy to learn. Scala developers appreciate using the concise and familiar functional programming patterns. The large community developing Play applications provides an excellent resource for getting questions answered.

Play is non-opinionated about database access, and integrates with many object relational mapping (ORM) layers.  It supports Anorm, Ebean, Slick, and JPA, out-of-the-box but many customers use NoSQL or REST layers and other ORMs. 

Read on to learn about:

* [Choosing a build tool](#Choosing-a-build-tool)
* [Verifying prerequisites](#Verifying-prerequisites) 

Or jump to [[Examples and templates|NewApplication]]

## Choosing a build tool

Because Play Framework libraries are available from [Maven Repository](https://mvnrepository.com/artifact/com.typesafe.play), you can use any Java build tool to build a Play project. However,   [sbt](http://www.scala-sbt.org/) (simple build tool) provides the development experience Play is known and loved for, such as routes, template compilation, and auto-reloading.  For example,  `sbt run` builds and runs an HTTP server and your application so that you can immediately view your work. 

This guide describes how to use `sbt` to develop Play applications. Examples demonstrate use of a Bash command shell, but work with any shell.

## Verifying prerequisites

Play requires Java SE 1.8 and we recommend that you use the latest version of `sbt`.  

To check your JDK, enter the following in a command window:

```bash
java -version
```

You should see something like:

```
java version "1.8.0_121"
Java(TM) SE Runtime Environment (build 1.8.0_121-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.121-b13, mixed mode)
```

If you don't have the right JDK, install it from [Oracle's JDK Site](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

To check your `sbt` version, enter the following:

```bash
sbt -version
```

The [sbt download page](http://www.scala-sbt.org/download.html) lists the latest version.

## Congratulations!

You are now ready to work with Play!  Next, learn more about Play from example apps or start developing your own application.