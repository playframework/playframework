<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# WebSockets

[WebSockets](https://en.wikipedia.org/wiki/WebSocket) are sockets that can be used from a web browser based on a protocol that allows two way full duplex communication.  The client can send messages and the server can receive messages at any time, as long as there is an active WebSocket connection between the server and the client.

Modern HTML5 compliant web browsers natively support WebSockets via a JavaScript WebSocket API.  However WebSockets are not limited in just being used by WebBrowsers, there are many WebSocket client libraries available, allowing for example servers to talk to each other, and also native mobile apps to use WebSockets.  Using WebSockets in these contexts has the advantage of being able to reuse the existing TCP port that a Play server uses.

> **Tip:** Check [caniuse.com](https://caniuse.com/#feat=websockets) to see more about which browsers supports WebSockets, known issues and more information.

## Handling WebSockets

Until now, we were using `Action` instances to handle standard HTTP requests and send back standard HTTP responses. WebSockets are a totally different beast and can’t be handled via standard `Action`.

Play's WebSocket handling mechanism is built around Akka streams.  A WebSocket is modelled as a `Flow`, incoming WebSocket messages are fed into the flow, and messages produced by the flow are sent out to the client.

Note that while conceptually, a flow is often viewed as something that receives messages, does some processing to them, and then produces the processed messages - there is no reason why this has to be the case, the input of the flow may be completely disconnected from the output of the flow.  Akka streams provides a constructor, `Flow.fromSinkAndSource`, exactly for this purpose, and often when handling WebSockets, the input and output will not be connected at all.

Play provides some factory methods for constructing WebSockets in [WebSocket](api/scala/play/api/mvc/WebSocket$.html).

## Handling WebSockets with Akka Streams and actors

To handle a WebSocket with an actor, we can use a Play utility, [ActorFlow](api/scala/play/api/libs/streams/ActorFlow$.html) to convert an `ActorRef` to a flow.  This utility takes a function that converts the `ActorRef` to send messages to a `akka.actor.Props` object that describes the actor that Play should create when it receives the WebSocket connection:

@[actor-accept](code/ScalaWebSockets.scala)

Note that `ActorFlow.actorRef(...)` can be replaced with any Akka Streams `Flow[In, Out, _]`, but actors are generally the most straightforward way to do it.

The actor that we're sending to here in this case looks like this:

@[example-actor](code/ScalaWebSockets.scala)

Any messages received from the client will be sent to the actor, and any messages sent to the actor supplied by Play will be sent to the client.  The actor above simply sends every message received from the client back with `I received your message: ` prepended to it.

### Detecting when a WebSocket has closed

When the WebSocket has closed, Play will automatically stop the actor.  This means you can handle this situation by implementing the actors `postStop` method, to clean up any resources the WebSocket might have consumed.  For example:

@[actor-post-stop](code/ScalaWebSockets.scala)

### Closing a WebSocket

Play will automatically close the WebSocket when your actor that handles the WebSocket terminates.  So, to close the WebSocket, send a `PoisonPill` to your own actor:

@[actor-stop](code/ScalaWebSockets.scala)

### Rejecting a WebSocket

Sometimes you may wish to reject a WebSocket request, for example, if the user must be authenticated to connect to the WebSocket, or if the WebSocket is associated with some resource, whose id is passed in the path, but no resource with that id exists.  Play provides `acceptOrResult` to address this, allowing you to return either a result (such as forbidden, or not found), or the actor to handle the WebSocket with:

@[actor-try-accept](code/ScalaWebSockets.scala)

> **Note**: the WebSocket protocol does not implement [Same Origin Policy](https://en.wikipedia.org/wiki/Same-origin_policy), and so does not protect against [Cross-Site WebSocket Hijacking](http://www.christian-schneider.net/CrossSiteWebSocketHijacking.html).  To secure a websocket against hijacking, the `Origin` header in the request must be checked against the server's origin, and manual authentication (including CSRF tokens) should be implemented.  If a WebSocket request does not pass the security checks, then `acceptOrResult` should reject the request by returning a Forbidden result.

### Handling different types of messages

So far we have only seen handling `String` frames.  Play also has built in handlers for `Array[Byte]` frames, and `JsValue` messages parsed from `String` frames.  You can pass these as the type parameters to the WebSocket creation method, for example:

@[actor-json](code/ScalaWebSockets.scala)

You may have noticed that there are two type parameters, this allows us to handle differently typed messages coming in to messages going out.  This is typically not useful with the lower level frame types, but can be useful if you parse the messages into a higher level type.

For example, let's say we want to receive JSON messages, and we want to parse incoming messages as `InEvent` and format outgoing messages as `OutEvent`.  The first thing we want to do is create JSON formats for out `InEvent` and `OutEvent` types:

@[actor-json-formats](code/ScalaWebSockets.scala)

Now we can create a WebSocket `MessageFlowTransformer` for these types:

@[actor-json-frames](code/ScalaWebSockets.scala)

And finally, we can use these in our WebSocket:

@[actor-json-in-out](code/ScalaWebSockets.scala)

Now in our actor, we will receive messages of type `InEvent`, and we can send messages of type `OutEvent`.

## Handling WebSockets with Akka streams directly

Actors are not always the right abstraction for handling WebSockets, particularly if the WebSocket behaves more like a stream.

@[streams1](code/ScalaWebSockets.scala)

A `WebSocket` has access to the request headers (from the HTTP request that initiates the WebSocket connection), allowing you to retrieve standard headers and session data. However, it doesn’t have access to a request body, nor to the HTTP response.

In this example we are creating a simple sink that prints each message to console. To send messages, we create a simple source that will send a single **Hello!** message.  We also need to concatenate a source that will never send anything, otherwise our single source will terminate the flow, and thus the connection.

> **Tip:** You can test WebSockets on <https://www.websocket.org/echo.html>. Just set the location to `ws://localhost:9000`.

Let’s write another example that discards the input data and closes the socket just after sending the **Hello!** message:

@[streams2](code/ScalaWebSockets.scala)

Here is another example in which the input data is logged to standard out and then sent back to the client using a mapped flow:

@[streams3](code/ScalaWebSockets.scala)

## Configuring WebSocket Frame Length

You can configure the max length for [WebSocket data frames](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers#Format) using `play.server.websocket.frame.maxLength` or passing `-Dwebsocket.frame.maxLength` system property when running your application. For example:

```
sbt -Dwebsocket.frame.maxLength=64k run
```

This configuration gives you more control of WebSocket frame length and can be adjusted to your application requirements. It may also reduce denial of service attacks using long data frames.