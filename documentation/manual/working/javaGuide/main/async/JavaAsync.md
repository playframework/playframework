<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Handling asynchronous results

## Make controllers asynchronous

Internally, Play Framework is asynchronous from the bottom up. Play handles every request in an asynchronous, non-blocking way.

The default configuration is tuned for asynchronous controllers. In other words, the application code should avoid blocking in controllers, i.e., having the controller code wait for an operation. Common examples of such blocking operations are JDBC calls, streaming API, HTTP requests and long computations.

Although it's possible to increase the number of threads in the default execution context to allow more concurrent requests to be processed by blocking controllers, following the recommended approach of keeping the controllers asynchronous makes it easier to scale and to keep the system responsive under load.

## Creating non-blocking actions

Because of the way Play works, action code must be as fast as possible, i.e., non-blocking. So what should we return from our action if we are not yet able to compute the result? We should return the *promise* of a result!

Java 8 provides a generic promise API called [`CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html).  A `CompletionStage<Result>` will eventually be redeemed with a value of type `Result`. By using a `CompletionStage<Result>` instead of a normal `Result`, we are able to return from our action quickly without blocking anything. Play will then serve the result as soon as the promise is redeemed.

The web client will be blocked while waiting for the response, but nothing will be blocked on the server, and server resources can be used to serve other clients.

## How to create a `CompletionStage<Result>`

To create a `CompletionStage<Result>` we need another promise first: the promise that will give us the actual value we need to compute the result:

@[promise-pi](code/javaguide/async/JavaAsync.java)

Play asynchronous API methods give you a `CompletionStage`. This is the case when you are calling an external web service using the `play.libs.WS` API, or if you are using Akka to schedule asynchronous tasks or to communicate with Actors using `play.libs.Akka`.

A simple way to execute a block of code asynchronously and to get a `CompletionStage` is to use the `CompletableFuture.supplyAsync()` helper:

@[promise-async](code/javaguide/async/JavaAsync.java)

> **Note:** It's important to understand which thread code runs on which promises. Here, the intensive computation will just be run on another thread.
>
> You can't magically turn synchronous IO into asynchronous by wrapping it in a `CompletionStage`. If you can't change the application's architecture to avoid blocking operations, at some point that operation will have to be executed, and that thread is going to block. So in addition to enclosing the operation in a `CompletionStage`, it's necessary to configure it to run in a separate execution context that has been configured with enough threads to deal with the expected concurrency. See [[Understanding Play thread pools|ThreadPools]] for more information.
>
> It can also be helpful to use Actors for blocking operations. Actors provide a clean model for handling timeouts and failures, setting up blocking execution contexts, and managing any state that may be associated with the service. Also Actors provide patterns like `ScatterGatherFirstCompletedRouter` to address simultaneous cache and database requests and allow remote execution on a cluster of backend servers. But an Actor may be overkill depending on what you need.

## Async results

We have been returning `Result` up until now. To send an asynchronous result our action needs to return a `CompletionStage<Result>`:

@[async](code/javaguide/async/controllers/Application.java)

## Actions are asynchronous by default

Play [[actions|JavaActions]] are asynchronous by default. For instance, in the controller code below, the returned `Result` is internally enclosed in a promise:

@[simple-action](../http/code/javaguide/http/JavaActions.java)

> **Note:** Whether the action code returns a `Result` or a `CompletionStage<Result>`, both kinds of returned object are handled internally in the same way. There is a single kind of `Action`, which is asynchronous, and not two kinds (a synchronous one and an asynchronous one). Returning a `CompletionStage` is a technique for writing non-blocking code.
