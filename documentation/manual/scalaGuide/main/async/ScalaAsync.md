# Handling asynchronous results

## Why asynchronous results?

Until now, we were able to generate the result to send to the web client directly. However, this is not always the case: the result might depend on an expensive computation or on a long web service call.

Because of the way Play works, the action code must be as fast as possible (ie. non blocking). So what should we return as result if we are not yet able to generate it? The response is a future result! 

A `Future[Result]` will eventually be redeemed with a value of type `Result`. By giving a `Future[Result]` instead of a normal `Result`, we are able to quickly generate the result without blocking. Then, Play will serve this result as soon as the promise is redeemed. 

The web client will be blocked while waiting for the response, but nothing will be blocked on the server, and server resources can be used to serve other clients.

## How to create a `Future[Result]`

To create a `Future[Result]` we need another future first: the future that will give us the actual value we need to compute the result:

@[future-result](code/ScalaAsync.scala)

All of Play’s asynchronous API calls give you a `Future`. This is the case whether you are calling an external web service using the `play.api.libs.WS` API, or using Akka to schedule asynchronous tasks or to communicate with actors using `play.api.libs.Akka`.

Here is a simple way to execute a block of code asynchronously and to get a `Future`:

@[intensive-computation](code/ScalaAsync.scala)

It's important to understand which thread code runs on with futures.  In the two code blocks above, there is an import on Plays default execution context.  This is an implicit parameter that gets passed to all methods on the future API that accept callbacks.  The execution context will often be equivalent to a thread pool, though not necessarily.

## Returning futures

While we were using the `Action.apply` builder methods to build actions until now, to send an asynchronous result, we need to use the `Action.async` buider method:

@[async-result](code/ScalaAsync.scala)

## Handling time-outs

It is often useful to handle time-outs properly, to avoid having the web browser block and wait if something goes wrong. You can easily compose a promise with a promise timeout to handle these cases:

@[timeout](code/ScalaAsync.scala)

> **Next:** [[Streaming HTTP responses | ScalaStream]]