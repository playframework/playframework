# Handling asynchronous results

## Why asynchronous results?

Until now, we were able to compute the result to send to the web client directly. This is not always the case: the result may depend of an expensive computation or on a long web service call.

Because of the way Play 2.0 works, action code must be as fast as possible (i.e. non blocking). So what should we return as result if we are not yet able to compute it? The response should be a promise of a result!

A `Promise<Result>` will eventually be redeemed with a value of type `Result`. By giving a `Promise<Result>` instead of a normal `Result`, we are able to compute the result quickly without blocking anything. Play will then serve this result as soon as the promise is redeemed. 

The web client will be blocked while waiting for the response but nothing will be blocked on the server, and server resources can be used to serve other clients.

## How to create a `Promise<Result>`

To create a `Promise<Result>` we need another promise first: the promise that will give us the actual value we need to compute the result:

```
Promise<Double> promiseOfPIValue = computePIAsynchronously();
Promise<Result> promiseOfResult = promiseOfPIValue.map(
  new Function<Double,Result>() {
    public Result apply(Double pi) {
      return ok("PI value computed: " + pi);
    } 
  }
);
```

> **Note:** Writing functional composition in Java is really verbose for the moment, but it should be better when Java supports [[lambda notation| http://mail.openjdk.java.net/pipermail/lambda-dev/2011-September/003936.html]].

Play 2.0 asynchronous API methods give you a `Promise`. This is the case when you are calling an external web service using the `play.libs.WS` API, or if you are using Akka to schedule asynchronous tasks or to communicate with Actors using `play.libs.Akka`.

A simple way to execute a block of code asynchronously and to get a `Promise` is to use the `play.libs.Akka` helpers:

```
Promise<Integer> promiseOfInt = Akka.future(
  new Callable<Integer>() {
    public Integer call() {
      return intensiveComputation();
    }
  }
);
```

> **Note:** Here, the intensive computation will just be run on another thread. It is also possible to run it remotely on a cluster of backend servers using Akka remote.

## AsyncResult

While we were using `Results.Status` until now, to send an asynchronous result we need an `Results.AsyncResult` that wraps the actual result:

```java
public static Result index() {
  Promise<Integer> promiseOfInt = play.libs.Akka.future(
    new Callable<Integer>() {
      public Integer call() {
        return intensiveComputation();
      }
    }
  );
  return async(
    promiseOfInt.map(
      new Function<Integer,Result>() {
        public Result apply(Integer i) {
          return ok("Got result: " + i);
        } 
      }
    )
  );
}
```

> **Note:** `async()` is an helper method building an `AsyncResult` from a `Promise<Result>`.

> **Next:** [[Streaming HTTP responses | JavaStream]]