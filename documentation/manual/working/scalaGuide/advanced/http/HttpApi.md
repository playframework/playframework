<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Introduction to Play HTTP API

## What is EssentialAction?

The EssentialAction is the new simpler type replacing the old Action[A]. To understand EssentialAction we need to understand the Play architecture.

The core of Play2 is really small, surrounded by a fair amount of useful APIs, services and structure to make Web Programming tasks easier.

Basically, Play2 is an API that abstractly have the following type:

```scala
RequestHeader -> Array[Byte] -> Result 
```

The above [computation](http://www.haskell.org/arrows/) takes the request header `RequestHeader`, then takes the request body as `Array[Byte]` and produces a `Result`.

Now this type presumes putting request body entirely into memory (or disk), even if you only want to compute a value out of it, or better forward it to a storage service like Amazon S3.

We rather want to receive request body chunks as a stream and be able to process them progressively if necessary.

What we need to change is the second arrow to make it receive its input in chunks and eventually produce a result. There is a type that does exactly this, it is called `Iteratee` and takes two type parameters.

`Iteratee[E,R]` is a type of [arrow](http://www.haskell.org/arrows/) that will take its input in chunks of type `E` and eventually return `R`. For our API we need an Iteratee that takes chunks of `Array[Byte]` and eventually return a `Result`. So we slightly modify the type to be:

```scala
RequestHeader -> Iteratee[Array[Byte],Result]
```

For the first arrow, we are simply using the Function[From,To] which could be type aliased with `=>`:

```scala
RequestHeader => Iteratee[Array[Byte],Result]
```

Now if I define an infix type alias for `Iteratee[E,R]`:

`type ==>[E,R] = Iteratee[E,R]` then I can write the type in a funnier way:

```scala
RequestHeader => Array[Byte] ==> Result
```

And this should read as: Take the request headers, take chunks of `Array[Byte]` which represent the request body and eventually return a `Result`. This exactly how the `EssentialAction` type is defined:

```scala
trait EssentialAction extends (RequestHeader => Iteratee[Array[Byte], Result])
```

The `Result` type, on the other hand, can be abstractly thought of as the response headers and the body of the response:

```scala
case class Result(headers: ResponseHeader, body:Array[Byte])
```

But, what if we want to send the response body progressively to the client without filling it entirely into memory. We need to improve our type. We need to replace the body type from an `Array[Byte]` to something that produces chunks of `Array[Byte]`. 

We already have a type for this and is called `Enumerator[E]` which means that it is capable of producing chunks of `E`, in our case `Enumerator[Array[Byte]]`: 

```scala
case class Result(headers:ResponseHeaders, body:Enumerator[Array[Byte]])
```

If we don't have to send the response progressively we still can send the entire body as a single chunk.

We can stream and write any type of data to socket as long as it is convertible to an `Array[Byte]`, that is what `Writeable[E]` insures for a given type 'E':

```scala
case class Result[E](headers:ResponseHeaders, body:Enumerator[E])(implicit writeable:Writeable[E])
```

## Bottom Line

The essential Play2 HTTP API is quite simple:

```scala
RequestHeader -> Iteratee[Array[Byte],Result]
```
or the funnier

```scala
RequestHeader => Array[Byte] ==> Result
```

Which reads as the following: Take the `RequestHeader` then take chunks of `Array[Byte]` and return a response. A response consists of `ResponseHeaders` and a body which is chunks of values convertible to `Array[Byte]` to be written to the socket represented in the `Enumerator[E]` type.
