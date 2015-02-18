<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Reactive Streams integration (experimental)

> **Play experimental libraries are not ready for production use**. APIs may change. Features may not work properly.

[Reactive Streams](http://www.reactive-streams.org/) is a specification and SPI that is currently under development. Reactive Streams provides a common standard that allows different stream implementations to be connected together. The SPI is quite small, with just a few simple interfaces such as `Publisher` and `Subscriber`.

Play 2.4 provides an **experimental** Reactive Streams integration module that adapts `Future`s, `Promise`s, `Enumerator`s and `Iteratee`s into Reactive Streams' `Publisher`s and `Subscriber`s.

## Known issues

* The implementations haven't been fully updated to verson 0.4 of the Reactive Streams specification. For example, `Publisher`s and `Subscriber`s may send or accept an `onComplete` event without a preceding `onSubscribe` event. This was allowed in 0.3 but is not permitted in 0.4.
* May need to lift `Input` events into the stream to ensure that `Input.EOF` events cannot be lost and to provide proper support for `Input.Empty`. At the moment there is the potential for event loss when adapting iteratees and enumerators.
* No performance tuning has been done.
* Needs support for two-way conversion between all the main stream and iteratee types.
* Documentation is limited.
* Test that the module works from Java.

## Usage

Include the Reactive Streams integration library into your project.

```scala
libraryDependencies += "com.typesafe.play" %% "play-streams-experimental" % "%PLAY_VERSION%"
```

All access to the module is through the [`Streams`](api/scala/index.html#play.play.api.libs.streams.Streams) object.

Here is an example that adapts a `Future` into a single-element `Publisher`.

```scala
val fut: Future[Int] = Future { ... }
val pubr: Publisher[Int] = Streams.futureToPublisher(fut)
```

See the `Streams` object's [API documentation](api/scala/index.html#play.play.api.libs.streams.Streams) for more information.
