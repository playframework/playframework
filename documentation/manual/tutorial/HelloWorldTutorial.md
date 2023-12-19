<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Hello World Tutorial

This tutorial describes how Play applications work, and shows you how to create a page that displays a customized Hello World greeting. 

You can use any Java build tool to build a Play project. This tutorial demonstrates sbt and Gradle because they both provide the development experience Play is known and loved for, such as auto-reloading, clear error messages, and template compilation. The tutorial procedures assume use of `sbt` or `gradlew` commands from a terminal, but you can also integrate Play projects with your favorite [[IDE]].

## Starting the project

Before following the tutorial instructions:

1. Make sure you have verified the [[requirements for running Play|Requirements]]
1. Obtain the appropriate sample project. You can either clone the [play-samples GitHub repository](https://github.com/playframework/play-samples) or download its contents [as zip file](https://github.com/playframework/play-samples/archive/refs/heads/3.0.x.zip).
    1. [Play Java Starter Example](https://github.com/playframework/play-samples/tree/3.0.x/play-java-starter-example)
    1. [Play Scala Starter Example](https://github.com/playframework/play-samples/tree/3.0.x/play-scala-starter-example)
1. Run the example following the steps in the `README.md` file inside the folder of the appropriate sample project.

 
> **Note**: When you run the tutorial application, it displays web pages with the same content and instructions contained here in the documentation. The tutorial includes a deliberate mistake and having the documentation and application pages open in different tabs or browsers allows you to consult the documentation for the fix when you encounter the error.

## Introduction to Play

As illustrated below, Play is a full-stack framework with all of the components you need to build a Web Application or a REST service, including: an integrated HTTP server, form handling, Cross-Site Request Forgery (CSRF) protection, a powerful routing mechanism, I18n support, and more. Play integrates with many object relational mapping (ORM) layers. It supports [[Anorm]], [[Ebean|JavaEbean]], [[Slick|PlaySlick]], and [[JPA|JavaJPA]] out-of-the-box, but many customers use NoSQL, other ORMs or even access data from a REST service.

[[images/play-stack.svg]]

Play APIs are available in both Java and Scala. The Framework uses [Pekko](https://pekko.apache.org/) and [Pekko HTTP](https://pekko.apache.org/docs/pekko-http/current/) under the hood. This endows Play applications with a stateless, non-blocking, event-driven architecture that provides horizontal and vertical scalability and uses resources more efficiently. Play projects contain Scala components, but because Play has a Java API, Java developers do not need to learn Scala to use Play successfully.

Here are just a few of the reasons developers love using Play Framework:

- Its Model-View-Controller (MVC) architecture is familiar and easy to learn.
- Direct support of common web development tasks and hot reloading saves precious development time.
- A large active community promotes knowledge sharing.
- [Twirl templates](https://github.com/playframework/twirl) render pages. The Twirl template language is:
    - Easy to learn
    - Requires no special editor
    - Provides type safety
    - Is compiled so that errors display in the browser

To learn more about Play's benefits, see Play's [[Introduction]] and [[Philosophy]]. Now, let's dive into what a Play application looks like.
