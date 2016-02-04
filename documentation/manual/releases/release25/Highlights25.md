<!--- Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com> -->
# What's new in Play 2.5

This page highlights the new features of Play 2.5. If you want learn about the changes you need to make to migrate to Play 2.5, check out the [[Play 2.5 Migration Guide|Migration25]].

**TODO: Write introduction that summarises the main themes of this release.**

**TODO: Review all headings to make them clear, succinct and consistent. If heading names change, check that backlinks from the Migration Guide still work.**

## New streaming API based on Akka Streams

**TODO: write this section**

- may want to break this into several sections

- talk about what a streaming API is and does

- talk about change to use Akka Streams

- talk about benefits vs iteratees

- can finally write streams use from Java code
  - iteratees not compatible with Java

- talk about any plans to deprecate/remove iteratees in the future

- body parsers, filters, WS, WebSockets

- explain performance improvements for no-body request and chunked responses

- link to information in migration docs

## Java API updated to use Java 8 classes

When Play 2.0 was released in 2012 Java had little support for Play's style of asynchronous functional programming. There were no lambdas, futures only had a blocking interface and common functional classes didn't exist. Play provided its own classes to fill the gap.

With Play 2.5 that situation has changed. Java 8 now ships with much better support for Play's style of programming. In Play 2.5 the Java APIs have been revamped to use standard Java 8 classes. This means that Play applications will integrate better with other Java libraries and look more like idiomatic Java.

Here are the main changes:

* Use Java functional interfaces (`Runnable`, `Consumer`, `Predicate`, etc). [[(See Migration Guide.)|Migration25#Replaced-functional-types-with-Java-8-functional-types]]
* Use Java 8's [`Optional`](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) instead of Play's `F.Option`. [[(See Migration Guide.)|Migration25#Replaced-F.Option-with-Java-8s-Optional]]
* Use Java 8's [`CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) instead of Play's `F.Promise`. [[(See Migration Guide.)|Migration25#Replaced-F.Promise-with-Java-8s-CompletionStage]]

## Support for other logging frameworks

Many of our users want to use their own choice of logging framework but this was not possible until Play 2.5. Now Play's fixed dependency on [Logback](http://logback.qos.ch/) has been removed and Play applications can now use any [SLF4J](http://www.slf4j.org/)-compatible logging framework. Logback is included by default, but you can disable it by including a setting in your `build.sbt` file and replace it with your own choice of framework. See Play's [[docs about logging|SettingsLogger#Using-a-Custom-Logging-Framework]] for more information about using other logging frameworks in Play.

Play applications will need to make a small change to their configuration because one Play's Logback classes has moved to a separate package as part of the change. [[(See Migration Guide.)|Migration25#Change-to-Logback-configuration]]

## Logging SQL statements

Play now has an easy way to log SQL statements, built on [jdbcdslog](https://github.com/jdbcdslog/jdbcdslog), that works across all JDBC databases, connection pool implementations and persisence frameworks (Anorm, Ebean, JPA, Slick, etc). When you enable logging you will see each SQL statement sent to your database as well as performance information about how long the statement takes to run.

For more information about how to use SQL logging, see the Play [[Java|JavaDatabase#How-to-configure-SQL-log-statement]] and [[Scala|ScalaDatabase#How-to-configure-SQL-log-statement]] database documentation.

**TODO: Thank person who contributed this feature and link to their Github page.**
## Netty native socket transport

If you run Play server on Linux you can now get a performance boost by using the [native socket feature](http://netty.io/wiki/native-transports.html) that was introdued in Netty 4.0.

You can learn how to use native sockets in Play documentation on [[configuring Netty|SettingsNetty#Configuring-transport-socket]].

**TODO: Thank person who contributed this feature and link to their Github page.**

## Improved seed template

**TODO: Explain what a seed template is and the changes that have been made.**

## ScalaTest is default test framework

**TODO: Explain reasons, benefits. Talk about plans for Specs. Do we need a section in migration?**