<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Handling asynchronous results

## Make controllers asynchronous

Internally, Play Framework is asynchronous from the bottom up. Play handles every request in an asynchronous, non-blocking way.

The default configuration is tuned for asynchronous controllers. In other words, the application code should avoid blocking in controllers, i.e., having the controller code wait for an operation. Common examples of such blocking operations are JDBC calls, streaming API, HTTP requests and long computations.

Although it's possible to increase the number of threads in the default execution context to allow more concurrent requests to be processed by blocking controllers, following the recommended approach of keeping the controllers asynchronous makes it easier to scale and to keep the system responsive under load.

## Creating non-blocking actions

Because of the way Play works, action code must be as fast as possible, i.e., non-blocking. So what should we return from our action if we are not yet able to compute the result? We should return the *promise* of a result!

Java 8 provides a generic promise API called [`CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html).  A `CompletionStage<Result>` will eventually be redeemed with a value of type `Result`. By using a `CompletionStage<Result>` instead of a normal `Result`, we are able to return from our action quickly without blocking anything.  Play will then serve the result as soon as the promise is redeemed.

## How to create a `CompletionStage<Result>`

To create a `CompletionStage<Result>` we need another promise first: the promise that will give us the actual value we need to compute the result:

@[promise-pi](code/javaguide/async/JavaAsync.java)

Play asynchronous API methods give you a `CompletionStage`. This is the case when you are calling an external web service using the `play.libs.WS` API, or if you are using Akka to schedule asynchronous tasks or to communicate with Actors using `play.libs.Akka`.

In this case, using `CompletionStage.thenApply` will execute the completion stage in the same calling thread as the previous task.  This is fine when you have a small amount of CPU bound logic with no blocking.

A simple way to execute a block of code asynchronously and to get a `CompletionStage` is to use the `CompletionStage.supplyAsync()` method:

@[promise-async](code/javaguide/async/JavaAsync.java)

Using `supplyAsync` creates a new task which will be placed on the fork join pool, and may be called from a different thread -- although, here it's using the default executor, and in practice you will specify an executor explicitly.

> Only the "\*Async" methods from `CompletionStage` provide asynchronous execution.

## Using HttpExecutionContext

You must supply the HTTP execution context explicitly as an executor when using a Java `CompletionStage` inside an [[Action|JavaActions]], to ensure that the `HTTP.Context` remains in scope.  If you don't supply the HTTP execution context, you'll get "There is no HTTP Context available from here" errors when you call `request()` or other methods that depend on `Http.Context`.

You can supply the [`play.libs.concurrent.HttpExecutionContext`](api/java/play/libs/concurrent/HttpExecutionContext.html) instance through dependency injection:

@[http-execution-context](../../../commonGuide/configuration/code/detailedtopics/httpec/MyController.java)

Please see [[Java thread locals|ThreadPools#Java thread locals]] for more information on using Java thread locals and HttpExecutionContext.

## Using CustomExecutionContext and HttpExecution

Using a `CompletionStage` or an `HttpExecutionContext` is only half of the picture though! At this point you are still on Play's default ExecutionContext.  If you are calling out to a blocking API such as JDBC, then you still will need to have your ExecutionStage run with a different executor, to move it off Play's rendering thread pool.  You can do this by creating a subclass of [`play.libs.concurrent.CustomExecutionContext`](api/java/play/libs/concurrent/CustomExecutionContext.html) with a reference to the [custom dispatcher](https://doc.akka.io/docs/akka/2.5/dispatchers.html?language=java).

Add the following imports:

@[async-explicit-ec-imports](code/javaguide/async/controllers/Application.java)

Define a custom execution context:

@[custom-execution-context](code/javaguide/async/controllers/MyExecutionContext.java)

You will need to define a custom dispatcher in `application.conf`, which is done [through Akka dispatcher configuration](https://doc.akka.io/docs/akka/2.5/dispatchers.html?language=java#setting-the-dispatcher-for-an-actor).

Once you have the custom dispatcher, add in the explicit executor and wrap it with [`HttpException.fromThread`](api/java/play/libs/concurrent/HttpExecution.html#fromThread-java.util.concurrent.Executor-):

@[async-explicit-ec](code/javaguide/async/controllers/Application.java)

> You can't magically turn synchronous IO into asynchronous by wrapping it in a `CompletionStage`. If you can't change the application's architecture to avoid blocking operations, at some point that operation will have to be executed, and that thread is going to block. So in addition to enclosing the operation in a `CompletionStage`, it's necessary to configure it to run in a separate execution context that has been configured with enough threads to deal with the expected concurrency. See [[Understanding Play thread pools|ThreadPools]] for more information, and download the [play example templates](https://playframework.com/download#examples) that show database integration.

## Actions are asynchronous by default

Play [[actions|JavaActions]] are asynchronous by default. For instance, in the controller code below, the returned `Result` is internally enclosed in a promise:

@[simple-action](../http/code/javaguide/http/JavaActions.java)

> **Note:** Whether the action code returns a `Result` or a `CompletionStage<Result>`, both kinds of returned object are handled internally in the same way. There is a single kind of `Action`, which is asynchronous, and not two kinds (a synchronous one and an asynchronous one). Returning a `CompletionStage` is a technique for writing non-blocking code.

## Handling time-outs

It is often useful to handle time-outs properly, to avoid having the web browser block and wait if something goes wrong. You can use the [`play.libs.concurrent.Futures.timeout`](api/java/play/libs/concurrent/Futures.html) method to wrap a `CompletionStage` in a non-blocking timeout.

@[timeout](code/javaguide/async/JavaAsync.java)

> **Note:** Timeout is not the same as cancellation -- even in case of timeout, the given future will still complete, even though that completed value is not returned.

