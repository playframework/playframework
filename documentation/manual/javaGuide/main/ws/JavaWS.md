# The Play WS API

Sometimes you want to call other HTTP services from within a Play application. Play supports this via its `play.libs.WS` library, which provides a way to make asynchronous HTTP calls.

A call made by `play.libs.WS` should return a `Promise<WS.Response>`, which you can handle later with Play’s asynchronous mechanisms.

## Imports

To use WS, first import the following packages:

@[ws-imports](code/javaguide/ws/JavaWS.java)

## Making HTTP calls

To make an HTTP request, you start with `WS.url()` to specify the URL. Then you get a builder that you can use to specify HTTP options, such as setting headers. You end by calling a method corresponding to the HTTP method you want to use:

@[get-call](code/javaguide/ws/JavaWS.java)

Alternatively:

@[post-call](code/javaguide/ws/JavaWS.java)

## Recovery

If you want to recover from an error in the call transparently, you can use `recover` to substitute a response:

@[get-call-and-recover](code/javaguide/ws/JavaWS.java)

## Retrieving the HTTP response result

The call is made asynchronously and you need to manipulate it as a `Promise<WS.Response>` to get the actual content. You can compose several promises and end up with a `Promise<Result>` that can be handled directly by the Play server:

@[simple-call](code/javaguide/ws/JavaWS.java)


## Composing results

If you want to make multiple calls in sequence, this can be achieved using `flatMap`:

@[composed-call](code/javaguide/ws/JavaWS.java)

## Configuring the HTTP client

The HTTP client can be configured globally in `application.conf` via a few properties:

@[application](code/javaguide/ws/application.conf)

> **Next:** [[Integrating with Akka | JavaAkka]]