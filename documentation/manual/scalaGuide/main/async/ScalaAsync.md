# Handling asynchronous results

## Why asynchronous results?

Until now, we were able to generate the result to send to the web client directly. However, this is not always the case: the result might depend on an expensive computation or of a long web service call.

Because of the way Play 2.0 works, the action code must be as fast as possible (ie. non blocking). So what should we return as result if we are not yet able to generate it? The response is a promise of result! 

A `Promise[Result]` will eventually be redeemed with a value of type `Result`. By giving a `Promise[Result]` instead if a normal `Result`, we are able to quickly generate the result without blocking. Then, Play will serve this result as soon as the promise is redeemed. 

The web client will be blocked while waiting for the response, but nothing will be blocked on the server, and server resources can be used to serve other clients.

## How to create a `Promise[Result]`

To create a `Promise[Result]` we need another promise first: the promise that will give us the actual value we need to compute the result:

```scala
val promiseOfPIValue: Promise[Double] = computePIAsynchronously()
val promiseOfResult: Promise[Result] = promiseOfPIValue.map { pi =>
  Ok("PI value computed: " + pi)    
}
```

All of Play 2.0’s asynchronous API calls give you a `Promise`. This is the case whether you are calling an external web service using the `play.api.libs.WS` API, or using Akka to schedule asynchonous tasks or to communicate with actors using `play.api.libs.Akka`.

A simple way to execute a block of code asynchronously and to get a `Promise` is to use the `play.api.libs.concurrent.Akka` helpers:

```scala
val promiseOfInt: Promise[Int] = Akka.future {
  intensiveComputation()
}
```

> **Note:** Here, the intensive computation will just be run on another thread. It is also possible to run it remotely on a cluster of backend servers using Akka remote.

## AsyncResult

While we were using `SimpleResult` until now, to send an asynchronous result, we need an `AsyncResult` to wrap the actual `SimpleResult`:

```scala
def index = Action {
  val promiseOfInt = Akka.future { intensiveComputation() }
  Async {
    promiseOfInt.map(i => Ok("Got result: " + i))
  }
}
```

> **Note:** `Async { }` is an helper method that builds an `AsyncResult` from a `Promise[Result]`.

## Handling time-outs

It is often useful to handle time-outs properly, to avoid having the web browser block and wait if something goes wrong. You can easily compose a promise with a promise timeout to handle these cases:

```scala
def index = Action {
  val promiseOfInt = Akka.future { intensiveComputation() }
  Async {
    promiseOfInt.orTimeout("Oops", 1000).map { eitherIntorTimeout =>
      eitherIorTimeout.fold(
        i => Ok("Got result: " + i),
        timeout => InternalServerError(timeout)
      )    
    }  
  }
}
```

> **Next:** [[Streaming HTTP responses | ScalaStream]]