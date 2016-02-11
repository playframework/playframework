# Streams Migration Guide

Play 2.5 has made several major changes to how it streams data and response bodies.

1. Play 2.5 uses **Akka Streams for streaming**. Previous versions of Play used iteratees for streaming as well as several other ad-hoc types of streaming, such as `WebSocket`, `Chunks`, etc.

    There are two main benefits of the change to use Akka Streams. First, Java users can now access the full features of Play, e.g. writing body parsers and filters. Second, the streaming library is now more consistent across Play.

2. Play 2.5 uses **`ByteString` to hold packets of bytes**. Previously, Play usied byte arrays (`byte[]`/`ArrayByte`) to hold bytes.

    The `ByteString` class is immutable like Java's `String`, so it's safer and easier to use. Like `String` it does have a small performance cost, because it copies its data when it is constructed, but this is balanced by its cheap concatenation and substring operations.

3. Play 2.5 has a new **`HttpEntity` type for response bodies**. Previously response bodies were a plain stream of bytes. HTTP bodies are now a type of `HttpEntity`: `Strict`, `Streamed` or `Chunked`.

    By telling Play what type of entity to use, applications can have more control over how Play sends HTTP responses. It also makes it easier for Play to optimize how it delivers the body.

## Summary of changes

The following parts of the Play API have been updated:

- results (`Result` body, `chunked`/`feed`/`stream` methods)
- actions (`EssentialAction`)
- body parsing (`BodyParser`)
- WebSockets (`WebSocket`)
- Server-Sent Events (`EventSource`)

The following types have been changed:

| **Purpose** | **Old types** | **New type**
| -------
| Holding bytes  |  `byte[]`/`Array[Byte]`  |  `ByteString`
| Producing a stream  | `Enumerator`, `WebSocket.Out`, `Chunks.Out`, `EventSource.Out` |  `Source`
| Transforming a stream to another stream  |  `Enumeratee`  |  `Flow`
| Transforming a stream to a single value  |  `Iteratee`  |  `Accumulator`
| Consuming a stream  |  `Iteratee`  |  `Sink`


### How to migrate (by API)

The following section gives an overview of how to migrate code that uses different parts of the API.

#### Migrating chunked results (`chunked`, `Results.Chunked`)

In Play 2.4 you would create chunked results in Scala with an `Enumerator` and in Java with a `Results.Chunked` object. In Play 2.5 these parts of the API are still available, but they have been deprecated.

If you choose to migrate to the new API, you can create a chunked result by calling the `chunked` method on a `StatusHeader` object and providing an Akka Streams `Source` object for the stream of chunks.

More advanced users may prefer to explicitly create an `HttpEntity.Chunked` object and pass it into the `Result` object constructor.

* To learn how to migrate an Enumerator to a Source, see XXXX.
* To learn how to migrate a `Results.Chunked` to a Source, see XXXX.

#### Migrating streamed results (`feed`, `stream`) (Scala only)

In Play 2.4 Scala users could stream results by passing an `Enumerator` to the `feed` or `stream` method. (Java users didn't have a way to stream results, apart from chunked results.) The `feed` method streamed the `Enumerator`'s data then closed the connection. The `stream` method, either streamed or chunked the result and possibly closed the connection, depending on the HTTP version of the connection and the presence or absence of the `Content-Length` header.

In Play 2.5, the `stream` method has been removed and the `feed` method has been deprecated. You can choose whether or not to migrate the `feed` method to the new API. If you use the `stream` method your code will need to be changed.

The new API is to create a `Result` object directly and choose an `HttpEntity` to represent its body. For streamed results, you can use the `HttpEntity.Streamed` class. The `Streamed` class takes a `Source` as a body and an optional `Content-Length` header value. The `Source`'s content will be sent to the client. If the entity has a `Content-Length` header then the connection will be left open, otherwise it will be closed to signal the end of the stream.

* To learn how to migrate an Enumerator to a Source, see XXXX.

#### Migrating WebSockets (`WebSocket`)

In Play 2.4, a WebSocket's bidirectional stream was represented in Java with a pair of `WebSocket.In` and `WebSocket.Out` objects and in Scala with a pair of `Enumerator` and `Iteratee` objects. In Play 2.5, both Java and Scala now use an Akka Streams `Flow` to represent the bidirectional stream.

To migrate your WebSockets code in Play 2.5 you have two options.

The first option is to use the old Play API, which has been deprecated and renamed to `LegacyWebSocket`. This is the easiest option. You just need to change your code that refers to `WebSocket` to refer to `LegacyWebSocket` instead. The `LegacyWebSocket` class gives you an easy migration path from Play 2.4 to Play 2.5.

The second option is to change to the new Play API. To do this you'll need to change your WebSocket code to use an Akka Streams `Flow` object.

**TODO: Figure out WebSocket actor migration, if any**

##### Migrating Scala WebSockets

The Play 2.4 Scala WebSocket API requires an `Enumerator`/`Iteratee` pair that produces `In` objects and consumes `Out` objects. A pair of `FrameFormatter`s handle the job of getting the data out of the `In` and `Out` objects.

```scala
case class WebSocket[In, Out](f: RequestHeader => Future[Either[Result, (Enumerator[In], Iteratee[Out, Unit]) => Unit]])(implicit val inFormatter: WebSocket.FrameFormatter[In], val outFormatter: WebSocket.FrameFormatter[Out]) extends Handler {
```

```
trait FrameFormatter[A] {
  def transform[B](fba: B => A, fab: A => B): FrameFormatter[B]
}
```

The Play 2.5's Scala WebSocket API is built around a `Flow` of `Message`s. A `Message` (**TODO: link to `play.api.http.websocket.Message`) represents an [WebSocket frame](https://tools.ietf.org/html/rfc6455#section-5). The `MessageFlowTransformer` type handles transforming high-level objects, like JSON, XML and bytes into `Message` frames. A set of built-in implicit `MessageFlowTransformer`s are provided, and you can also write your own.

```
trait WebSocket extends Handler {
  def apply(request: RequestHeader): Future[Either[Result, Flow[Message, Message, _]]]
}
```

```
sealed trait Message
case class TextMessage(data: String) extends Message
case class BinaryMessage(data: ByteString) extends Message
case class CloseMessage(statusCode: Option[Int] = Some(CloseCodes.Regular), reason: String = "") extends Message
case class PingMessage(data: ByteString) extends Message
case class PongMessage(data: ByteString) extends Message
```

```
trait MessageFlowTransformer[+In, -Out] { self =>
  def transform(flow: Flow[In, Out, _]): Flow[Message, Message, _]
}
```

To migrate, you'll need to translate the bidirectional `Enumerator`/`Iteratee` stream into a `Flow`. You may also need to convert your `In`/`Out` objects into `Message`s using a `MessageFlowTransformer`, although this is not necessary for common types like JSON, since some built-in implicit conversions are provided.

* To learn how to combine a Source and Sink into a Flow, see XXXX.
* To learn how to migrate an Enumerator to a Source, see XXXX.
* To learn how to migrate an Iteratee to a Sink, see XXXX.

##### Migrating Java WebSockets

The Play 2.4 Java WebSocket API uses a `WebSocket.In` object to handle incoming messages and a `WebSocket.Out` object to send outgoing messages. The API supported WebSockets transporting text, bytes or JSON frames.

```java
return WebSocket.whenReady((in, out) -> {
    out.write("Hello!");
    out.close();
});
```

The new Play 2.5 API is much more powerful. You can now create a `WebSocket` and return arbitrary WebSocket `Message` frames. The bidirectional `Message` streams are represented as a `Flow`.

```java
public abstract class WebSocket {
    public abstract CompletionStage<F.Either<Result, Flow<Message, Message, ?>>> apply(Http.RequestHeader request);
}
```

If you want to convert WebSocket `Message` frames to your own types you can use the `MappedWebSocketAcceptor` class. Several of these classes are provided for you: `Text`, `Binary` and `Json`. For example:

```java
return WebSocket.Text.accept(requestHeader -> {
  // return a Flow<String, String, ?>
})
```

You can also create your own `MappedWebSocketAcceptor` by defining how to convert incoming outgoing messages.

* To learn how to combine a Source and Sink into a Flow, see XXXX.
* To learn how to migrate a `WebSocket.In` to a Sink, see XXXX.
* To learn how to migrate a `WebSocket.Out` to a Source, see XXXX.

#### Migrating Server-Sent events (`EventSource`)

**TODO**

#### Migrating Comet

**TODO: this still needs to be implemented**

#### Migrating custom actions (`EssentialAction`) (Scala only)

Most Scala users will use the `Action` class for their actions. The `Action` class is a type of `EssentialAction` that always parses its body fully before running its logic and sending a result. Some users may have written their own custom `EssentialAction`s so that they can do things like incrementally processing the request body.

If you're only using normal `Action`s in your Play 2.4 application then they do not need any migration. However, if you've written an `EssentialAction`, then you'll need to migrate it to the new API in Play 2.5. The behavior of an `EssentialAction` is still the same, but the signature has changed from Play 2.4:

```scala
trait EssentialAction extends (RequestHeader => Iteratee[Array[Byte], Result])
```

to a new signature in Play 2.5:

```scala
trait EssentialAction extends (RequestHeader => Accumulator[ByteString, Result])
```

To migrate, you'll need to replace your `Iteratee` with an `Accumulator` and your `Array[Byte]` with a `ByteString`.

* To learn how to migrate an Iteratee to an Accumulator, see XXXX.
* To learn how to migrate an `Array[Byte]` to a `ByteString` see XXXX.

#### Migrating custom body parsers (`BodyParser`) (Scala only)

If you're a Scala user who has a custom `BodyParser` in their Play 2.4 application then you'll need to migrate it to the new Play 2.5 API. The `BodyParser` trait signature looks like this in Play 2.4:

```scala
trait BodyParser[+A] extends (RequestHeader => Iteratee[Array[Byte], Either[Result, A]])
```

In Play 2.5 it has changed to use Akka Streams types:

```scala
trait BodyParser[+A] extends (RequestHeader => Accumulator[ByteString, Either[Result, A]])
```

To migrate, you'll need to replace your `Iteratee` with an `Accumulator` and your `Array[Byte]` with a `ByteString`.

* To learn how to migrate an Iteratee to an Accumulator, see XXXX.
* To learn how to migrate an `Array[Byte]` to a `ByteString` see XXXX.

#### Migrating `Result` bodies (Scala only)

The `Result` object has changed how it represents thre result body and the connection close flag. Instead of taking `body: Enumerator[Array[Byte]], connection: Connection`, it now takes `body: HttpEntity`. The `HttpEntity` type contains information about the body and implicit information about how to close the connection.

You can migrate your existing `Enumerator` by using a `Streamed` entity that contains a `Source` and an optional `Content-Length` and `Content-Type` header.

```scala
val bodyPublisher: Publisher[ByteString] = Streams.enumeratorToPublisher(bodyEnumerator)
val bodySource: Source[ByteString, _] = Source.fromPublisher(bodyPublisher)
val entity: HttpEntity = HttpEntity.Streamed(bodySource)
new Result(headers, entity)
```

See the section on migrating `Enumerator`s and migrating to `ByteString` for more information on migrating to these types.

* To learn how to migrate an Iteratee to an Accumulator, see XXXX.
* To learn how to migrate an `Array[Byte]` to a `ByteString` see XXXX.

You may find that you don't need a stream for the `Result` body at all. If that's the case you might want to use a `Strict` entity for the body.

```scala
new Result(headers, HttpEntity.Strict(bytes))
```

### How to migrate (by type)

This section explains how to migrate your byte arrays and streams to the new Akka Streams APIs.

Akka Streams is part of the Akka project. Play uses Akka Streams to provide streaming functionality: sending and receiving sequences of bytes and other objects. The Akka project has a lot of good documentation about Akka Streams. Before you start using Akka Streams in Play it is worth looking at the Akka Streams documentation to see what information is available.

* [Documentation for Java](http://doc.akka.io/docs/akka/2.4.2-RC2/java/stream/index.html)
* [Documentation for Scala](http://doc.akka.io/docs/akka/2.4.2-RC2/scala/stream/index.html)

The API documentation can be found under the `akka.stream` package in the main Akka API documentation:

* [Akka Javadoc](http://doc.akka.io/japi/akka/2.4.2-RC2/)
* [Akka Scala](http://doc.akka.io/api/akka/2.4.2-RC2/)

When you're first getting started with Akka Streams, the *Basics and working with Flows* section of the Akka documentation is worth a look. It will introduce you to the most important parts of the Akka Streams API.

* [Basics for Java](http://doc.akka.io/docs/akka/2.4.2-RC2/java/stream/stream-flows-and-basics.html)
* [Basics for Scala](http://doc.akka.io/docs/akka/2.4.2-RC2/scala/stream/stream-flows-and-basics.html)

You don't need to convert your whole application in one go. Parts of your application can keep using iteratees while other parts use 

**TODO: Explain how to change iteratees to streams. Explain the deprecation/removal plan for iteratees. Do we need separate sections for result streaming and HttpEntities, body parsers and Accumulators, WS and WebSockets?**

#### Migrating byte arrays (`byte[]`/`Array[Byte]`) to `ByteString`s

Refer to the [Java](http://doc.akka.io/japi/akka/2.4.2-RC2/index.html) and [Scala](http://doc.akka.io/api/akka/2.4.2-RC2/index.html#akka.util.ByteString) API documentation for `ByteString`.

Examples:

Scala:

```scala
// Get the empty ByteString (this instance is cached)
ByteString.empty
// Create a ByteString from a String
ByteString("hello")
ByteString.fromString("hello")
// Create a ByteString from an Array[Byte]
ByteString(arr)
ByteString.fromArray(arr)
```

Java:

```java
// Get the empty ByteString (this instance is cached)
ByteString.empty();
// Create a ByteString from a String
ByteString.fromString("hello");
// Create a ByteString from an Array[Byte]
ByteString.fromArray(arr);
```

#### Migrating `*.Out`s to `Source`s

Play now uses a `Source` to generate events instead of its old `WebSocket.Out`, `Chunks.Out` and `EventSource.Out` classes. These classes were simple to use, but they were inflexible and they didn't implement [back](http://doc.akka.io/docs/akka/2.4.2-RC2/java/stream/stream-flows-and-basics.html#back-pressure-explained) [pressure](http://doc.akka.io/docs/akka/2.4.2-RC2/scala/stream/stream-flows-and-basics.html#back-pressure-explained) properly.

You can replace your `*.Out` class with any `Source` that produces a stream. There are lots of ways to create `Source`s ([Java](http://doc.akka.io/docs/akka/2.4.2-RC2/java/stream/stream-flows-and-basics.html#Defining_sources__sinks_and_flows)/[Scala](http://doc.akka.io/docs/akka/2.4.2-RC2/scala/stream/stream-flows-and-basics.html#Defining_sources__sinks_and_flows).

If you want to replace your `*.Out` with a simple object that you can write messages to and then close, without worrying about back pressure, then you can use the `Source.actorRef` method:

**TODO: Write Java code**

Scala:
```scala
val source = Source.actorRef[ByteString](256, OverflowStrategy.dropNew).mapMaterializedValue { sourceActor =>
  sourceActor ! ByteString("hello")
  sourceActor ! ByteString("world")
  sourceActor ! Status.Success(()) // close the source
}
```

#### Migrating `Enumerator`s to `Source`s

Play uses `Enumerator`s in many places to produce streams of values.

**Step 1:** Use transitional API (if available)

If you use `Results.chunked` or `Results.feed` you can continue to use the existing methods. These methods have been deprecated, so you may want to change your code anyway.

**Step 2:** Convert `Enumerator` to `Source` with an adapter

You can convert your existing `Enumerator to ` 

**Step 3:** (Optional) Rewrite to a `Source`

#### Migrating `Iteratee`s

**Step 1:** Convert using an adapter

**Step 2:** (Optional) Rewrite to a `Sink`

#### Migrating `Enumeratees`s

- Server-Sent Events (`EventSource`)

**Step 1:** Convert using an adapter

**Step 2:** (Optional) Rewrite to a `Sink`
