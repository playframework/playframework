<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Reactive Streams integration (experimental)

> **Play experimental libraries are not ready for production use**. APIs may change. Features may not work properly.

[Reactive Streams](http://www.reactive-streams.org/) is a new standard that gives a common API for asynchronous streams. Play 2.4 introduces some wrappers to convert Play's [[Iteratees and Enumerators|Iteratees]] into Reactive Streams objects. This means that Play can integrate with other software that supports Reactive Streams, e.g. [Akka Streams](https://doc.akka.io/docs/akka/2.4.3/scala/stream/index.html), [RxJava](https://github.com/ReactiveX/RxJavaReactiveStreams) and [others](http://www.reactive-streams.org/announce-1.0.0#implementations).

The purpose of the API is:

* to check that the Reactive Streams API is powerful enough to express Play iteratees and enumerators
* to test integration between Play and Akka Streams
* to provide stream conversions needed by the experimental [[Akka HTTP server backend|AkkaHttpServer]]
* to test out an API.

This API is **highly experimental**. It should be reasonably free of bugs, but its methods and classes and concepts are very likely to change in the future.

## Known issues

* No Java API. This shouldn't be hard to implement, but it hasn't been done yet.
* The implementation hasn't been tested against the Reactive Streams test suite so there may be some conformance issues.
* May need to lift `Input` events into the stream to ensure that `Input.EOF` events cannot be lost and to provide proper support for `Input.Empty`. At the moment there is the potential for event loss when adapting iteratees and enumerators.
* No performance tuning has been done.
* Needs support for two-way conversion between all the main stream and iteratee types.
* Documentation is limited.

## Usage

Include the Reactive Streams integration library into your project.

```scala
libraryDependencies += "com.typesafe.play" %% "play-streams-experimental" % "%PLAY_VERSION%"
```

All access to the module is through the `Streams` object.

Here is an example that adapts a `Future` into a single-element `Publisher`.

```scala
val fut: Future[Int] = Future { ... }
val pubr: Publisher[Int] = Streams.futureToPublisher(fut)
```

See the `Streams` object's API documentation for more information.

For more examples you can look at the code used by the experimental [[Akka HTTP server backend|AkkaHttpServer]]. Here are the main files where you can find examples:

* [ModelConversion](https://github.com/playframework/playframework/blob/2.4.x/framework/src/play-akka-http-server/src/main/scala/play/core/server/akkahttp/ModelConversion.scala)
* [AkkaStreamsConversion](https://github.com/playframework/playframework/blob/2.4.x/framework/src/play-akka-http-server/src/main/scala/play/core/server/akkahttp/AkkaStreamsConversion.scala)
* [AkkaHttpServer](https://github.com/playframework/playframework/blob/2.4.x/framework/src/play-akka-http-server/src/main/scala/play/core/server/akkahttp/AkkaHttpServer.scala)
