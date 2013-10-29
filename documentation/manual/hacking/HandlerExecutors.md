<!--- Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com> -->
# Custom handler executors

> **Warning**: The Play Handler Executor API is experimental, and may change significantly between minor releases.

[`HandlerExecutor`](api/scala/index.html#play.core.system.HandlerExecutor) is a low level experimental plugin point for experimenting with various container level features that would otherwise be impossible with the Play API.  Handler executors are backend implementation specific, for example, a handler executor written for Netty won't work with Play running on top of akka-http.  They are not intended for general use, and only the built in handlers are officially supported.  However, in order to provide the opportunity to innovate, try new technologies and approaches to web development, Play provides these APIs to those who may find them useful.

The Play [`Routes`](api/scala/index.html#play.core.Router$$Routes) API is in essence a partial function that takes a [`RequestHeader`](api/scala/index.html#play.api.mvc.RequestHeader) and returns a [`Handler`](api/scala/index.html#play.api.mvc.Handler).  Play provides two `Handler` implementations out of the box, [`EssentialAction`](api/scala/index.html#play.api.mvc.EssentialAction), and [`WebSocket`](api/scala/index.html#play.api.mvc.WebSocket).

## Use cases

The following use cases are examples of what may be possible with this API:

* HTTP protocol upgrades.  For example, custom WebSockets handling, SPDY/HTTP2 support, HTTP CONNECT support.
* Direct access to SSL engines for custom SSL requirements.
* Use of different IO APIs or request/response APIs.

## Execution model

A Play application has a list of handler executors.  These are invoked on each request on the handler returned by the router sequentially, until one of them returns a result.  The order that handler executors are invoked is undefined, except that built in handler executors always come last.

The handler executor takes a number of parameters:

* [`HandlerExecutorContext`](api/scala/index.html#play.core.system.HandlerExecutorContext) - This is the context for the running application.  It includes the application, if it exists, as well as the list of handler executors.  It can be used by handler executors if they have a nested handler, to invoke that handler.  It can also be used to handle handler executor level errors.
* `RequestHeader` - This is the request header returned by `Global.onRequestReceived`.
* `BackendRequest` - This a backend implementation specific object containing the objects necessary for handling the request.  In the case of Netty, it includes the `ChannelHandlerContext` and `MessageEvent` that contains the HTTP request.
* `Handler` - This is the handler that the handler executor is to attempt to handle, if it knows how to handle it.

Handlers return `Option[Future[_]]`.  If they are able to handle the request, this should be `Some`, otherwise it should be `None`.  The future should be a future that is redeemed when the handler has completely finished handling the request, including sending the result.  The backend implementation may add some callbacks to it to do some cleanup at the end of the request.

## Handler executor discovery

New handler executors can be discovered by declaring a backend implementation specific descriptor.  The descriptor should contain the classnames of any handler executors provided, one per line.  In the case of netty, this descriptor is called `play.netty.handlers`.

## Example implementation

The following example shows how a custom handler executor may be used to allow an application to terminate a connection outside of the normal HTTP request response flow.  The first thing to define is a new Handler type, and a companion object that can be used to easily create actions:

@[closeable-handler](code/HandlerExecutors.scala)

Now we write the handler executor to execute this handler:

@[closeable-handler-executor](code/HandlerExecutors.scala)

Play needs to know how to find this handler, so we create a file called `play.netty.handlers`, and place it in there:

```
com.foo.CloseableHandlerExecutor$
```

Note that since the our handler executor is an object, we append the `$` to the end of it, since that's what Scala compiles objects to.

We're now ready to write an action that uses this custom handler:

@[closeable-action](code/HandlerExecutors.scala)
