# Handling asynchronous results

## Why asynchronous results?

Until now, we have been able to compute the result to send to the web client directly. This is not always the case: the result may depend on an expensive computation or on a long web service call.

Because of the way Play works, action code must be as fast as possible (i.e. non blocking). So what should we return as result if we are not yet able to compute it? The response should be a promise of a result!

A `Promise<Result>` will eventually be redeemed with a value of type `Result`. By giving a `Promise<Result>` instead of a normal `Result`, we are able to compute the result quickly without blocking anything. Play will then serve this result as soon as the promise is redeemed. 

The web client will be blocked while waiting for the response but nothing will be blocked on the server, and server resources can be used to serve other clients.

## How to create a `Promise<Result>`

To create a `Promise<Result>` we need another promise first: the promise that will give us the actual value we need to compute the result:

@[promise-pi](code/javaguide/async/JavaAsync.java)

> **Note:** Writing functional composition in Java is really verbose at the moment, but it should be better when Java supports [lambda notation](http://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html).

Play asynchronous API methods give you a `Promise`. This is the case when you are calling an external web service using the `play.libs.WS` API, or if you are using Akka to schedule asynchronous tasks or to communicate with Actors using `play.libs.Akka`.

A simple way to execute a block of code asynchronously and to get a `Promise` is to use the `promise()` helper:

@[promise-async](code/javaguide/async/JavaAsync.java)

> **Note:** Here, the intensive computation will just be run on another thread. It is also possible to run it remotely on a cluster of backend servers using Akka remote.

## AsyncResult

While we have been simply returning `Result` until now, to send an asynchronous result we need to return `Promise<Result>` from our action:

@[async](code/javaguide/async/Application.java)

> **Next:** [[Streaming HTTP responses | JavaStream]]
