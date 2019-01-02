<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# What's new in Play 2.5

This page highlights the new features of Play 2.5. If you want to learn about the changes you need to make to migrate to Play 2.5, check out the [[Play 2.5 Migration Guide|Migration25]].

## New streaming API based on Akka Streams

The main theme of Play 2.5 has been moving from Play's iteratee-based asynchronous IO API to [Akka Streams](https://doc.akka.io/docs/akka/2.4.3/scala/stream/stream-introduction.html).

At its heart, any time you communicate over the network, or write/read some data to the filesystem, some streaming is involved.  In many cases, this streaming is done at a low level, and the framework exposes the materialized values to your application as in-memory messages.  This is the case for many Play actions, a body parser converts the request body stream into an object such as a parsed JSON object, which the application consumes, and the returned result body is a JSON object that Play then turns back into a stream.

Traditionally, on the JVM, streaming is done using the blocking `InputStream` and `OutputStream` APIs.  These APIs require a dedicated thread to use them - when reading, that thread must block and wait for data, when writing, that thread must block and wait for the data to be flushed.  An asynchronous framework, such as Play, offers many advantages because it limits the resources it requires by not using blocking APIs such as these.  Instead, for streaming, an asynchronous API needs to be used, where the framework is notified that there's data to read or that data has been written, rather than having to have a thread block and wait for it.

Prior to Play 2.5, Play used Iteratees as this asynchronous streaming mechanism, but now it uses Akka Streams.

### Why not iteratees?

Iteratees are a functional approach to handling asynchronous streaming.  They are incredibly powerful, while also offering an incredibly small API surface area - the Iteratee API consists of one method, `fold`, the rest is just helpers built on top of this method.  Iteratees also provide a very high degree of safety, as long as your code compiles, it's very unlikely that you would have any bugs related to the implementation of an iteratee itself, such as concurrency or error handling, most bugs would be in the "business" logic of the iteratee.

While this safety and simplicity is great, the consequence of it was that it has a very steep learning curve.  Programming using iteratees requires a shift in thinking from traditional IO handling, and many developers find that the investment required to make this shift is too high for their IO needs.  Another disadvantage of iteratees is that they are practically unimplementable in Java, due to their reliance on many high level functional programming features.

### Why Akka Streams

Akka Streams provides a good balance between safety, simplicity and familiarity.  Akka Streams intentionally constrains you in what you can do so that you can only do things correctly, but not as much as iteratees do.  Conceptually they are much more familiar to most developers, offering both functional and imperative ways of working with them.  Akka Streams also has a first class Java API, making it simple to implement any streaming requirements in Java that are needed.

### Where are Akka Streams used?

The places where you will come across Akka Streams in your Play applications include:

* Filters
* Streaming response bodies
* Request body parsers
* WebSockets
* Streaming WS client responses

### Reactive Streams

[Reactive Streams](http://reactivestreams.org) is a new specification for asynchronous streaming, which is scheduled for inclusion in JDK9 and available as a standalone library for JDK6 and above.  In general, it is not an end-user library, rather it is an SPI that streaming libraries can implement in order to integrate with each other.  Both Akka Streams and iteratees provide a reactive streams SPI implementation.  This means, existing iteratees code can easily be used with Play's new Akka Streams support.  It also means any other reactive streams implementations can be used in Play.

### The future of iteratees

Iteratees still have some use cases where they shine.  At current, there is no plan to deprecate or remove them from Play, though they may be moved to a standalone library. Since iteratees provide a reactive streams implementation, they will always be usable in Play.

## Better control over WebSocket frames

The Play 2.5 WebSocket API gives you direct control over WebSocket frames. You can now send and receive binary, text, ping, pong and close frames. If you don't want to worry about this level of detail, Play will still automatically convert your JSON or XML data into the right kind of frame.

## New Java APIs

Play 2.5's Java APIs provide feature parity with the Scala APIs. We have introduced several new Java APIs to do this:

* [`HttpRequestHandler`](api/java/play/http/HttpRequestHandler.html), which allows interception of requests as they come in, before they are sent to the router.
* [`EssentialAction`](api/java/play/mvc/EssentialAction.html), a low level action used in `EssentialFilter` and `HttpRequestHandler`s.
* [`EssentialFilter`](api/java/play/mvc/EssentialFilter.html)/[`Filter`](api/java/play/mvc/Filter.html) for writing filters in Java.
* [`BodyParser`](api/java/play/mvc/BodyParser.html), which allows writing custom body parsers in Java.

## Java API updated to use Java 8 classes

When Play 2.0 was released in 2012, Java had little support for Play's style of asynchronous functional programming. There were no lambdas, futures only had a blocking interface and common functional classes didn't exist. Play provided its own classes to fill the gap.

With Play 2.5 that situation has changed. Java 8 now ships with much better support for Play's style of programming. In Play 2.5 the Java APIs have been revamped to use standard Java 8 classes. This means that Play applications will integrate better with other Java libraries and look more like idiomatic Java.

Here are the main changes:

* Use Java functional interfaces (`Runnable`, `Consumer`, `Predicate`, etc). [[(See Migration Guide.)|JavaMigration25#Replaced-functional-types-with-Java-8-functional-types]]
* Use Java 8's [`Optional`](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) instead of Play's `F.Option`. [[(See Migration Guide.)|JavaMigration25#Replaced-F.Option-with-Java-8s-Optional]]
* Use Java 8's [`CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) instead of Play's `F.Promise`. [[(See Migration Guide.)|JavaMigration25#Replaced-F.Promise-with-Java-8s-CompletionStage]]

## Support for other logging frameworks

Many of our users want to use their own choice of logging framework but this was not possible until Play 2.5. Now Play's fixed dependency on [Logback](https://logback.qos.ch/) has been removed and Play applications can now use any [SLF4J](https://www.slf4j.org/)-compatible logging framework. Logback is included by default, but you can disable it by including a setting in your `build.sbt` file and replace it with your own choice of framework. See Play's [[docs about logging|SettingsLogger#Using-a-Custom-Logging-Framework]] for more information about using other logging frameworks in Play.

Play applications will need to make a small change to their configuration because one Play's Logback classes has moved to a separate package as part of the change. See the [[Migration Guide|Migration25#Change-to-Logback-configuration]] for more details.

## Logging SQL statements

Play now has an easy way to log SQL statements, built on [jdbcdslog](https://github.com/jdbcdslog/jdbcdslog), that works across all JDBC databases, connection pool implementations and persistence frameworks (Anorm, Ebean, JPA, Slick, etc). When you enable logging you will see each SQL statement sent to your database as well as performance information about how long the statement takes to run.

For more information about how to use SQL logging, see the Play [[Java|JavaDatabase#How-to-configure-SQL-log-statement]] and [[Scala|ScalaDatabase#How-to-configure-SQL-log-statement]] database documentation.

## Netty native socket transport

If you run Play server on Linux you can now get a performance boost by using the [native socket feature](https://netty.io/wiki/native-transports.html) that was introduced in Netty 4.0.

You can learn how to use native sockets in Play documentation on [[configuring Netty|SettingsNetty#Configuring-transport-socket]].

## Performance Improvements

Thanks to various performance optimizations, Play 2.5's performance testing framework shows roughly 60K requests per second, an **almost 20% improvement over Play 2.4.x**.

## WS Improvements

Play WS has been upgraded to AsyncHttpClient 2.0, and now includes a request pipeline filter ([[Scala|ScalaWS#Request-Filters]], [[Java|JavaWS#Request-Filters]]) that can be used to log requests in [cURL format](https://curl.haxx.se/docs/manpage.html).  
