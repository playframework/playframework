<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->

# Hello World Tutorial

This tutorial describes how Play applications work, and shows you how to create a page that displays a customized Hello World greeting. 

You can use any Java build tool to build a Play project. This tutorial demonstrates sbt and Gradle because they both provide the development experience Play is known and loved for, such as auto-reloading, clear error messages, and template compilation. The tutorial procedures assume use of `sbt` or `gradlew` commands from a terminal, but you can also integrate Play projects with your favorite [[IDE]].

## Starting the project

Before following the tutorial instructions:

1. Make sure you have verified the [[requirements for running Play|Requirements]]
1. Obtain the appropriate example zip file:
    1. [Play Java Starter Example](https://developer.lightbend.com/start/?group=play&project=play-java-starter-example)
    1. [Play Scala Starter Example](https://developer.lightbend.com/start/?group=play&project=play-scala-starter-example)
1. Unzip and run the example following the steps in the `README.md` file.

## Introduction to Play

As illustrated below, Play is a full-stack framework with all of the components you need to build a Web Application or a REST service, including: an integrated HTTP server, form handling, Cross-Site Request Forgery (CSRF) protection, a powerful routing mechanism, I18n support, and more. Play integrates with many object relational mapping (ORM) layers. It supports [[Anorm]], [[Ebean|JavaEbean]], [[Slick|PlaySlick]], and [[JPA|JavaJPA]] out-of-the-box, but many customers use NoSQL, other ORMs or even access data from a REST service.

[[images/play-stack.png]]

Play APIs are available in both Java and Scala. The Framework uses [Akka](https://akka.io) and [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html) under the hood. This endows Play applications with a stateless, non-blocking, event-driven architecture that provides horizontal and vertical scalability and uses resources more efficiently. Play projects contain Scala components, but because Play has a Java API, you do not need to learn Scala to use Play successfully if you are a Java developer.

Here are just a few of the reasons developers love using Play Framework:

- Its Model-View-Controller (MVC) architecture is familiar and easy to learn.
- Direct support of common web development tasks and hot reloading saves precious development time.
- A large active community that promotes knowledge sharing.
- Use of [Twirl templates](https://github.com/playframework/twirl) to render pages. The Twirl template language is:
  - Easy to learn
  - Requires no special editor
  - Provides type safety
  - Is compiled so that errors display in the browser

To learn more about Play's benefits, see Play's [[Introduction]] and [[Philosophy]]. Now, let's dive into what a Play application looks like.
