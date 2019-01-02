<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Body parsers

## What is a body parser?

An HTTP request is a header followed by a body.  The header is typically small - it can be safely buffered in memory, hence in Play it is modelled using the [`RequestHeader`](api/java/play/mvc/Http.RequestHeader.html) class.  The body however can be potentially very long, and so is not buffered in memory, but rather is modelled as a stream.  However, many request body payloads are small and can be modelled in memory, and so to map the body stream to an object in memory, Play provides a [`BodyParser`](api/java/play/mvc/BodyParser.html) abstraction.

Since Play is an asynchronous framework, the traditional `InputStream` can't be used to read the request body - input streams are blocking, when you invoke `read`, the thread invoking it must wait for data to be available.  Instead, Play uses an asynchronous streaming library called [Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/index.html?language=java).  Akka Streams is an implementation of [Reactive Streams](http://www.reactive-streams.org/), a SPI that allows many asynchronous streaming APIs to seamlessly work together, so though traditional `InputStream` based technologies are not suitable for use with Play, Akka Streams and the entire ecosystem of asynchronous libraries around Reactive Streams will provide you with everything you need.

## Using the built in body parsers

Most typical web apps will not need to use custom body parsers, they can simply work with Play's built in body parsers.  These include parsers for JSON, XML, forms, as well as handling plain text bodies as Strings and byte bodies as `ByteString`.

### The default body parser

The default body parser that's used if you do not explicitly select a body parser will look at the incoming `Content-Type` header, and parses the body accordingly.  So for example, a `Content-Type` of type `application/json` will be parsed as a `JsonNode`, while a `Content-Type` of `application/x-www-form-urlencoded` will be parsed as a `Map<String, String[]>`.

The request body can be accessed through the `body()` method on [`Request`](api/java/play/mvc/Http.Request.html), and is wrapped in a [`RequestBody`](api/java/play/mvc/Http.RequestBody.html) object, which provides convenient accessors to the various types that the body could be.  For example, to access a JSON body:

@[access-json-body](code/javaguide/http/JavaBodyParsers.java)

The following is a mapping of types supported by the default body parser:

- **`text/plain`**: `String`, accessible via `asText()`.
- **`application/json`**: `com.fasterxml.jackson.databind.JsonNode`, accessible via `asJson()`.
- **`application/xml`**, **`text/xml`** or **`application/XXX+xml`**: `org.w3c.Document`, accessible via `asXml()`.
- **`application/x-www-form-urlencoded`**: `Map<String, String[]>`, accessible via `asFormUrlEncoded()`.
- **`multipart/form-data`**: [`MultipartFormData`](api/java/play/mvc/Http.MultipartFormData.html), accessible via `asMultipartFormData()`.
- Any other content type: [`RawBuffer`](api/java/play/mvc/Http.RawBuffer.html), accessible via `asRaw()`.

The default body parser tries to determine if the request has a body before it tries to parse. According to the HTTP spec, the presence of either the `Content-Length` or `Transfer-Encoding` header signals the presence of a body, so the parser will only parse if one of those headers is present, or on `FakeRequest` when a non-empty body has explicitly been set.

If you would like to try to parse a body in all cases, you can use the `AnyContent` body parser, described [below](#Choosing-an-explicit-body-parser).

### Choosing an explicit body parser

If you want to explicitly select a body parser, this can be done using the [`@BodyParser.Of`](api/java/play/mvc/BodyParser.Of.html) annotation, for example:

@[particular-body-parser](code/javaguide/http/JavaBodyParsers.java)

The body parsers that Play provides out of the box are all inner classes of the [`BodyParser`](api/java/play/mvc/BodyParser.html) class.  Briefly, they are:

- [`Default`](api/java/play/mvc/BodyParser.Default.html): The default body parser.
- [`AnyContent`](api/java/play/mvc/BodyParser.AnyContent.html): Like the default body parser, but will parse bodies of `GET`, `HEAD` and `DELETE` requests.
- [`Json`](api/java/play/mvc/BodyParser.Json.html): Parses the body as JSON.
- [`TolerantJson`](api/java/play/mvc/BodyParser.TolerantJson.html): Like `Json`, but does not validate that the `Content-Type` header is JSON.
- [`Xml`](api/java/play/mvc/BodyParser.Xml.html): Parses the body as XML.
- [`TolerantXml`](api/java/play/mvc/BodyParser.TolerantXml.html): Like `Xml`, but does not validate that the `Content-Type` header is XML.
- [`Text`](api/java/play/mvc/BodyParser.Text.html): Parses the body as a String.
- [`TolerantText`](api/java/play/mvc/BodyParser.TolerantText.html): Like `Text`, but does not validate that the `Content-Type` is `text/plain`.
- [`Bytes`](api/java/play/mvc/BodyParser.Bytes.html): Parses the body as a `ByteString`.
- [`Raw`](api/java/play/mvc/BodyParser.Raw.html): Parses the body as a `RawBuffer`.  This will attempt to store the body in memory, up to Play's configured memory buffer size, but fallback to writing it out to a `File` if that's exceeded.
- [`FormUrlEncoded`](api/java/play/mvc/BodyParser.FormUrlEncoded.html): Parses the body as a form.
- [`MultipartFormData`](api/java/play/mvc/BodyParser.MultipartFormData.html): Parses the body as a multipart form, storing file parts to files.
- [`Empty`](api/java/play/mvc/BodyParser.Empty.html): Does not parse the body, rather it ignores it.

### Content length limits

Most of the built in body parsers buffer the body in memory, and some buffer it on disk.  If the buffering was unbounded, this would open up a potential vulnerability to malicious or careless use of the application.  For this reason, Play has two configured buffer limits, one for in memory buffering, and one for disk buffering.

The memory buffer limit is configured using `play.http.parser.maxMemoryBuffer`, and defaults to 100KB, while the disk buffer limit is configured using `play.http.parser.maxDiskBuffer`, and defaults to 10MB.  These can both be configured in `application.conf`, for example, to increase the memory buffer limit to 256KB:

    play.http.parser.maxMemoryBuffer = 256K

You can also limit the amount of memory used on a per action basis by writing a custom body parser, see [below](#Writing-a-custom-max-length-body-parser) for details.

## Writing a custom body parser

A custom body parser can be made by implementing the [`BodyParser`](api/java/play/mvc/BodyParser.html) class.  This class has one abstract method:

@[body-parser-apply](code/javaguide/http/JavaBodyParsers.java)

The signature of this method may be a bit daunting at first, so let's break it down.

The method takes a [`RequestHeader`](api/java/play/mvc/Http.RequestHeader.html).  This can be used to check information about the request - most commonly, it is used to get the `Content-Type`, so that the body can be correctly parsed.

The return type of the method is an [`Accumulator`](api/java/play/libs/streams/Accumulator.html).  An accumulator is a thin layer around an [Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/index.html?language=java) [`Sink`](https://doc.akka.io/japi/akka/2.5/akka/stream/javadsl/Sink.html).  An accumulator asynchronously accumulates streams of elements into a result, it can be run by passing in an Akka Streams [`Source`](https://doc.akka.io/japi/akka/2.5/akka/stream/javadsl/Source.html), this will return a `CompletionStage` that will be redeemed when the accumulator is complete.  It is essentially the same thing as a `Sink<E, CompletionStage<A>>`, in fact it is nothing more than a wrapper around this type, but the big difference is that `Accumulator` provides convenient methods such as `map`, `mapFuture`, `recover` etc. for working with the result as if it were a promise, where `Sink` requires all such operations to be wrapped in a `mapMaterializedValue` call.

The accumulator that the `apply` method returns consumes elements of type [`ByteString`](https://doc.akka.io/japi/akka/2.5/akka/util/ByteString.html) - these are essentially arrays of bytes, but differ from `byte[]` in that `ByteString` is immutable, and many operations such as slicing and appending happen in constant time.

The return type of the accumulator is `F.Either<Result, A>`.  This says it will either return a `Result`, or it will return a body of type `A`.  A result is generally returned in the case of an error, for example, if the body failed to be parsed, if the `Content-Type` didn't match the type that the body parser accepts, or if an in memory buffer was exceeded.  When the body parser returns a result, this will short circuit the processing of the action - the body parsers result will be returned immediately, and the action will never be invoked.

### Composing an existing body parser

As a first example, we'll show how to compose an existing body parser.  Let's say you want to parse some incoming JSON into a class that you have defined, called `Item`.

First we'll define a new body parser that depends on the JSON body parser:

@[composing-class](code/javaguide/http/JavaBodyParsers.java)

Now, in our implementation of the `apply` method, we'll invoke the JSON body parser, which will give us back the `Accumulator<ByteString, F.Either<Result, JsonNode>>` to consume the body.  We can then map that like a promise, to convert the parsed `JsonNode` body to a `User` body.  If the conversion fails, we return a `Left` of a `Result` saying what the error was:

@[composing-apply](code/javaguide/http/JavaBodyParsers.java)

The returned body will be wrapped in a `RequestBody`, and can be accessed using the `as` method:

@[composing-access](code/javaguide/http/JavaBodyParsers.java)

### Writing a custom max length body parser

Another use case may be to define a body parser that uses a custom maximum length for buffering.  Many of the built in Play body parsers are designed to be extended to allow overriding the buffer length in this way, for example, this is how the text body parser can be extended:

@[max-length](code/javaguide/http/JavaBodyParsers.java)

### Directing the body elsewhere

So far we've shown extending and composing the existing body parsers.  Sometimes you may not actually want to parse the body, you simply want to forward it elsewhere.  For example, if you want to upload the request body to another service, you could do this by defining a custom body parser:

@[forward-body](code/javaguide/http/JavaBodyParsers.java)

### Custom parsing using Akka Streams

In rare circumstances, it may be necessary to write a custom parser using Akka Streams.  In most cases it will suffice to buffer the body in a `ByteString` first, by composing the `Bytes` parser as described [above](#Composing-an-existing-body-parser), this will typically offer a far simpler way of parsing since you can use imperative methods and random access on the body.

However, when that's not feasible, for example when the body you need to parse is too long to fit in memory, then you may need to write a custom body parser.

A full description of how to use Akka Streams is beyond the scope of this documentation - the best place to start is to read the [Akka Streams documentation](https://doc.akka.io/docs/akka/2.5/stream/index.html?language=java).  However, the following shows a CSV parser, which builds on the [Parsing lines from a stream of ByteStrings](https://doc.akka.io/docs/akka/2.5/stream/stream-cookbook.html?language=java#parsing-lines-from-a-stream-of-bytestrings) documentation from the Akka Streams cookbook:

@[csv](code/javaguide/http/JavaBodyParsers.java)
