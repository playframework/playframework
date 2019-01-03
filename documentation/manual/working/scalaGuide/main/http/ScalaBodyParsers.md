<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Body parsers

## What is a body parser?

An HTTP request is a header followed by a body.  The header is typically small - it can be safely buffered in memory, hence in Play it is modelled using the [`RequestHeader`](api/scala/play/api/mvc/RequestHeader.html) class.  The body however can be potentially very long, and so is not buffered in memory, but rather is modelled as a stream.  However, many request body payloads are small and can be modelled in memory, and so to map the body stream to an object in memory, Play provides a [`BodyParser`](api/scala/play/api/mvc/BodyParser.html) abstraction.

Since Play is an asynchronous framework, the traditional `InputStream` can't be used to read the request body - input streams are blocking, when you invoke `read`, the thread invoking it must wait for data to be available.  Instead, Play uses an asynchronous streaming library called [Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/index.html?language=scala).  Akka Streams is an implementation of [Reactive Streams](http://www.reactive-streams.org/), a SPI that allows many asynchronous streaming APIs to seamlessly work together, so though traditional `InputStream` based technologies are not suitable for use with Play, Akka Streams and the entire ecosystem of asynchronous libraries around Reactive Streams will provide you with everything you need.

## More about Actions

Previously we said that an `Action` was a `Request => Result` function. This is not entirely true. Let’s have a more precise look at the `Action` trait:

@[action](code/ScalaBodyParsers.scala)

First we see that there is a generic type `A`, and then that an action must define a `BodyParser[A]`. With `Request[A]` being defined as:

@[request](code/ScalaBodyParsers.scala)

The `A` type is the type of the request body. We can use any Scala type as the request body, for example `String`, `NodeSeq`, `Array[Byte]`, `JsonValue`, or `java.io.File`, as long as we have a body parser able to process it.

To summarize, an `Action[A]` uses a `BodyParser[A]` to retrieve a value of type `A` from the HTTP request, and to build a `Request[A]` object that is passed to the action code.

## Using the built in body parsers

Most typical web apps will not need to use custom body parsers, they can simply work with Play's built in body parsers.  These include parsers for JSON, XML, forms, as well as handling plain text bodies as Strings and byte bodies as `ByteString`.

### The default body parser

The default body parser that's used if you do not explicitly select a body parser will look at the incoming `Content-Type` header, and parses the body accordingly.  So for example, a `Content-Type` of type `application/json` will be parsed as a `JsValue`, while a `Content-Type` of `application/x-www-form-urlencoded` will be parsed as a `Map[String, Seq[String]]`.

The default body parser produces a body of type [`AnyContent`](api/scala/play/api/mvc/AnyContent.html).  The various types supported by `AnyContent` are accessible via `as` methods, such as `asJson`, which returns an `Option` of the body type:

@[access-json-body](code/ScalaBodyParsers.scala)

The following is a mapping of types supported by the default body parser:

- **text/plain**: `String`, accessible via `asText`.
- **application/json**: [`JsValue`](https://static.javadoc.io/com.typesafe.play/play-json_2.12/2.6.9/play/api/libs/json/JsValue.html), accessible via `asJson`.
- **application/xml**, **text/xml** or **application/XXX+xml**: `scala.xml.NodeSeq`, accessible via `asXml`.
- **application/x-www-form-urlencoded**: `Map[String, Seq[String]]`, accessible via `asFormUrlEncoded`.
- **multipart/form-data**: [`MultipartFormData`](api/scala/play/api/mvc/MultipartFormData.html), accessible via `asMultipartFormData`.
- Any other content type: [`RawBuffer`](api/scala/play/api/mvc/RawBuffer.html), accessible via `asRaw`.

The default body parser tries to determine if the request has a body before it tries to parse. According to the HTTP spec, the presence of either the `Content-Length` or `Transfer-Encoding` header signals the presence of a body, so the parser will only parse if one of those headers is present, or on `FakeRequest` when a non-empty body has explicitly been set.

If you would like to try to parse a body in all cases, you can use the `anyContent` body parser, described [below](#Choosing-an-explicit-body-parser).

## Choosing an explicit body parser

If you want to explicitly select a body parser, this can be done by passing a body parser to the `Action` [`apply`](api/scala/play/api/mvc/ActionBuilder.html#apply[A]\(bodyParser:play.api.mvc.BodyParser[A]\)\(block:R[A]=%3Eplay.api.mvc.Result\):play.api.mvc.Action[A]) or [`async`](api/scala/play/api/mvc/ActionBuilder.html#async[A]\(bodyParser:play.api.mvc.BodyParser[A]\)\(block:R[A]=%3Escala.concurrent.Future[play.api.mvc.Result]\):play.api.mvc.Action[A]) method.

Play provides a number of body parsers out of the box, this is made available through the [`PlayBodyParsers`](api/scala/play/api/mvc/PlayBodyParsers.html) trait, which can be injected into your controller.

So for example, to define an action expecting a json body (as in the previous example):

@[body-parser-json](code/ScalaBodyParsers.scala)

Note this time that the type of the body is `JsValue`, which makes it easier to work with the body since it's no longer an `Option`.  The reason why it's not an `Option` is because the json body parser will validate that the request has a `Content-Type` of `application/json`, and send back a `415 Unsupported Media Type` response if the request doesn't meet that expectation.  Hence we don't need to check again in our action code.

This of course means that clients have to be well behaved, sending the correct `Content-Type` headers with their requests.  If you want to be a little more relaxed, you can instead use `tolerantJson`, which will ignore the `Content-Type` and try to parse the body as json regardless:

@[body-parser-tolerantJson](code/ScalaBodyParsers.scala)

Here is another example, which will store the request body in a file:

@[body-parser-file](code/ScalaBodyParsers.scala)

### Combining body parsers

In the previous example, all request bodies are stored in the same file. This is a bit problematic isn’t it? Let’s write another custom body parser that extracts the user name from the request Session, to give a unique file for each user:

@[body-parser-combining](code/ScalaBodyParsers.scala)

> **Note:** Here we are not really writing our own BodyParser, but just combining existing ones. This is often enough and should cover most use cases. Writing a `BodyParser` from scratch is covered in the advanced topics section.

### Max content length

Text based body parsers (such as **text**, **json**, **xml** or **formUrlEncoded**) use a max content length because they have to load all the content into memory.  By default, the maximum content length that they will parse is 100KB.  It can be overridden by specifying the `play.http.parser.maxMemoryBuffer` property in `application.conf`:

    play.http.parser.maxMemoryBuffer=128K

For parsers that buffer content on disk, such as the raw parser or `multipart/form-data`, the maximum content length is specified using the `play.http.parser.maxDiskBuffer` property, it defaults to 10MB.  The `multipart/form-data` parser also enforces the text max length property for the aggregate of the data fields.

You can also override the default maximum length for a given action:

@[body-parser-limit-text](code/ScalaBodyParsers.scala)

You can also wrap any body parser with `maxLength`:

@[body-parser-limit-file](code/ScalaBodyParsers.scala)

## Writing a custom body parser

A custom body parser can be made by implementing the [`BodyParser`](api/scala/play/api/mvc/BodyParser.html) trait.  This trait is simply a function:

@[body-parser](code/ScalaBodyParsers.scala)

The signature of this function may be a bit daunting at first, so let's break it down.

The function takes a [`RequestHeader`](api/scala/play/api/mvc/RequestHeader.html).  This can be used to check information about the request - most commonly, it is used to get the `Content-Type`, so that the body can be correctly parsed.

The return type of the function is an [`Accumulator`](api/scala/play/api/libs/streams/Accumulator.html).  An accumulator is a thin layer around an [Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/index.html?language=scala) [`Sink`](https://doc.akka.io/api/akka/2.5/index.html#akka.stream.scaladsl.Sink).  An accumulator asynchronously accumulates streams of elements into a result, it can be run by passing in an Akka Streams [`Source`](https://doc.akka.io/api/akka/2.5/index.html#akka.stream.scaladsl.Source), this will return a `Future` that will be redeemed when the accumulator is complete.  It is essentially the same thing as a `Sink[E, Future[A]]`, in fact it is nothing more than a wrapper around this type, but the big difference is that `Accumulator` provides convenient methods such as `map`, `mapFuture`, `recover` etc. for working with the result as if it were a promise, where `Sink` requires all such operations to be wrapped in a `mapMaterializedValue` call.

The accumulator that the `apply` method returns consumes elements of type [`ByteString`](https://doc.akka.io/api/akka/2.5/akka/util/ByteString.html) - these are essentially arrays of bytes, but differ from `byte[]` in that `ByteString` is immutable, and many operations such as slicing and appending happen in constant time.

The return type of the accumulator is `Either[Result, A]` - it will either return a `Result`, or it will return a body of type `A`.  A result is generally returned in the case of an error, for example, if the body failed to be parsed, if the `Content-Type` didn't match the type that the body parser accepts, or if an in memory buffer was exceeded.  When the body parser returns a result, this will short circuit the processing of the action - the body parsers result will be returned immediately, and the action will never be invoked.

### Directing the body elsewhere

One common use case for writing a body parser is for when you actually don't want to parse the body, rather, you want to stream it elsewhere.  To do this, you may define a custom body parser:

@[forward-body](code/ScalaBodyParsers.scala)

### Custom parsing using Akka Streams

In rare circumstances, it may be necessary to write a custom parser using Akka Streams.  In most cases it will suffice to buffer the body in a `ByteString` first, this will typically offer a far simpler way of parsing since you can use imperative methods and random access on the body.

However, when that's not feasible, for example when the body you need to parse is too long to fit in memory, then you may need to write a custom body parser.

A full description of how to use Akka Streams is beyond the scope of this documentation - the best place to start is to read the [Akka Streams documentation](https://doc.akka.io/docs/akka/2.5/stream/index.html?language=scala).  However, the following shows a CSV parser, which builds on the [Parsing lines from a stream of ByteStrings](https://doc.akka.io/docs/akka/2.5/stream/stream-cookbook.html?language=scala#parsing-lines-from-a-stream-of-bytestrings) documentation from the Akka Streams cookbook:

@[csv](code/ScalaBodyParsers.scala)
