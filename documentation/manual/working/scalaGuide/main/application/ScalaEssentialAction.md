<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Essential Action

## What is EssentialAction?

The EssentialAction is the simpler type underlying Action[A]. To understand EssentialAction we need to understand the Play architecture.

The core of Play is really small, surrounded by a fair amount of useful APIs, services and structure to make Web Programming tasks easier.

Basically, Play's action API abstractly has the following type:

```scala
RequestHeader -> Array[Byte] -> Result 
```

The above [computation](https://www.haskell.org/arrows/) takes the request header `RequestHeader`, then takes the request body as `Array[Byte]` and produces a `Result`.

Now this type presumes putting request body entirely into memory (or disk), even if you only want to compute a value out of it, or better forward it to a storage service like Amazon S3.

We rather want to receive request body chunks as a stream and be able to process them progressively if necessary.

What we need to change is the second arrow to make it receive its input in chunks and eventually produce a result. There is a type that does exactly this, it is called `Accumulator` and takes two type parameters.

`Accumulator[E,R]` is a type of [arrow](https://www.haskell.org/arrows/) that will take its input in chunks of type `E` and eventually return `R`. For our API we need an Accumulator that takes chunks of `ByteString` (essentially a more efficient wrapper for a byte array) and eventually return a `Result`. So we slightly modify the type to be:

```scala
RequestHeader -> Accumulator[ByteString, Result]
```

For the first arrow, we are simply using the Function[From, To] which could be type aliased with `=>`:

```scala
RequestHeader => Accumulator[ByteString, Result]
```

Now if I define an infix type alias for `Accumulator[E,R]`:

`type ==>[E,R] = Accumulator[E,R]` then I can write the type in a funnier way:

```scala
RequestHeader => ByteString ==> Result
```

And this should read as: Take the request headers, take chunks of `ByteString` which represent the request body and eventually return a `Result`. This exactly how the `EssentialAction` type is defined:

```scala
trait EssentialAction extends (RequestHeader => Accumulator[ByteString, Result])
```

The `Result` type, on the other hand, can be abstractly thought of as the response headers and the body of the response:

```scala
case class Result(header: ResponseHeader, body: ByteString)
```

But, what if we want to send the response body progressively to the client without filling it entirely into memory? We need to improve our type. We need to replace the body type from a `ByteString` to something that produces chunks of `ByteString`. 

We already have a type for this and is called `Source[E, _]` which means that it is capable of producing chunks of `E`, in our case `Source[ByteString, _]`: 

```scala
case class Result(header: ResponseHeader, body: Source[ByteString, _])
```

If we don't have to send the response progressively we still can send the entire body as a single chunk. In the actual API, Play supports different types of entities using the `HttpEntity` wrapper type, which supports streamed, chunked, and strict entities.

Note that Play's built-in helpers like `Ok(myObject)` convert `myObject` to an entity using an implicit `Writeable[E]` instance that creates an `HttpEntity` from the entity `E`.

## Bottom Line

The essential Play HTTP API is quite simple:

```scala
RequestHeader -> Accumulator[ByteString, Result]
```
or the funnier

```scala
RequestHeader => ByteString ==> Result
```

which reads as the following: Take the `RequestHeader` then take chunks of `ByteString` and return a response. A response consists of `ResponseHeaders` and a body which is chunks of values convertible to `ByteString` to be written to the socket represented in the `Source[E, _]` type.
