<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# WebSockets

[WebSockets](https://en.wikipedia.org/wiki/WebSocket) are sockets that can be used from a web browser based on a protocol that allows two way full duplex communication.  The client can send messages and the server can receive messages at any time, as long as there is an active WebSocket connection between the server and the client.

Modern HTML5 compliant web browsers natively support WebSockets via a JavaScript WebSocket API.  However WebSockets are not limited in just being used by WebBrowsers, there are many WebSocket client libraries available, allowing for example servers to talk to each other, and also native mobile apps to use WebSockets.  Using WebSockets in these contexts has the advantage of being able to reuse the existing TCP port that a Play server uses.

> **Tip:** Check [caniuse.com](https://caniuse.com/#feat=websockets) to see more about which browsers supports WebSockets, known issues and more information.

## Handling WebSockets

Until now, we were using `Action` instances to handle standard HTTP requests and send back standard HTTP responses. WebSockets are a totally different beast and can’t be handled via standard `Action`.

Play's WebSocket handling mechanism is built around Akka streams.  A WebSocket is modelled as a `Flow`, incoming WebSocket messages are fed into the flow, and messages produced by the flow are sent out to the client.

Note that while conceptually, a flow is often viewed as something that receives messages, does some processing to them, and then produces the processed messages - there is no reason why this has to be the case, the input of the flow may be completely disconnected from the output of the flow.  Akka streams provides a constructor, `Flow.fromSinkAndSource`, exactly for this purpose, and often when handling WebSockets, the input and output will not be connected at all.

Play provides some factory methods for constructing WebSockets in [WebSocket](api/java/play/mvc/WebSocket.html).

## Handling WebSockets with actors

To handle a WebSocket with an actor, we can use a Play utility, [ActorFlow](api/java/play/libs/streams/ActorFlow.html) to convert an `ActorRef` to a flow.  This utility takes a function that converts the `ActorRef` to send messages to a `akka.actor.Props` object that describes the actor that Play should create when it receives the WebSocket connection:

@[content](code/javaguide/async/websocket/HomeController.java)

The actor that we're sending to here in this case looks like this:

@[actor](code/javaguide/async/MyWebSocketActor.java)

Any messages received from the client will be sent to the actor, and any messages sent to the actor supplied by Play will be sent to the client.  The actor above simply sends every message received from the client back with `I received your message: ` prepended to it.

### Detecting when a WebSocket has closed

When the WebSocket has closed, Play will automatically stop the actor.  This means you can handle this situation by implementing the actors `postStop` method, to clean up any resources the WebSocket might have consumed.  For example:

@[actor-post-stop](code/javaguide/async/JavaWebSockets.java)

### Closing a WebSocket

Play will automatically close the WebSocket when your actor that handles the WebSocket terminates.  So, to close the WebSocket, send a `PoisonPill` to your own actor:

@[actor-stop](code/javaguide/async/JavaWebSockets.java)

### Rejecting a WebSocket

Sometimes you may wish to reject a WebSocket request, for example, if the user must be authenticated to connect to the WebSocket, or if the WebSocket is associated with some resource, whose id is passed in the path, but no resource with that id exists.  Play provides a `acceptOrResult` WebSocket builder for this purpose:

@[actor-reject](code/javaguide/async/JavaWebSockets.java)

> **Note**: the WebSocket protocol does not implement [Same Origin Policy](https://en.wikipedia.org/wiki/Same-origin_policy), and so does not protect against [Cross-Site WebSocket Hijacking](http://www.christian-schneider.net/CrossSiteWebSocketHijacking.html).  To secure a websocket against hijacking, the `Origin` header in the request must be checked against the server's origin, and manual authentication (including CSRF tokens) should be implemented.  If a WebSocket request does not pass the security checks, then `acceptOrResult` should reject the request by returning a Forbidden result.

### Accepting a WebSocket asynchronously

You may need to do some asynchronous processing before you are ready to create an actor or reject the WebSocket, if that's the case, you can simply return `CompletionStage<WebSocket<A>>` instead of `WebSocket<A>`.

### Handling different types of messages

So far we have only seen handling `String` frames, using the `Text` builder.  Play also has built in handlers for `ByteString` frames using the `Binary` builder, and `JSONNode` messages parsed from `String` frames using the `Json` builder.  Here's an example of using the `Json` builder:

@[actor-json](code/javaguide/async/JavaWebSockets.java)

Play also provides built in support for translating `JSONNode` messages to and from a higher level object.  If you had a class, `InEvent`, representing input events, and another class, `OutEvent`, representing output events, you could use it like this:

@[actor-json-class](code/javaguide/async/JavaWebSockets.java)

## Handling WebSockets using Akka streams directly

Actors are not always the right abstraction for handling WebSockets, particularly if the WebSocket behaves more like a stream.

Instead, you can use Akka streams directly to handle WebSockets.  To use Akka streams, first import the Akka streams javadsl:

@[streams-imports](code/javaguide/async/JavaWebSockets.java)

Now you can use it like so.

@[streams1](code/javaguide/async/JavaWebSockets.java)

A `WebSocket` has access to the request headers (from the HTTP request that initiates the WebSocket connection), allowing you to retrieve standard headers and session data. However, it doesn’t have access to a request body, nor to the HTTP response.

It this example we are creating a simple sink that prints each message to console. To send messages, we create a simple source that will send a single **Hello!** message.  We also need to concatenate a source that will never send anything, otherwise our single source will terminate the flow, and thus the connection.

> **Tip:** You can test WebSockets on <https://www.websocket.org/echo.html>. Just set the location to `ws://localhost:9000`.

Let’s write another example that discards the input data and closes the socket just after sending the **Hello!** message:

@[streams2](code/javaguide/async/JavaWebSockets.java)

Here is another example in which the input data is logged to standard out and then sent back to the client using a mapped flow:

@[streams3](code/javaguide/async/JavaWebSockets.java)

## Configuring WebSocket Frame Length

You can configure the max length for [WebSocket data frames](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers#Format) using `play.server.websocket.frame.maxLength` or passing `-Dwebsocket.frame.maxLength` system property when running your application. For example:

```
sbt -Dwebsocket.frame.maxLength=64k run
```

This configuration gives you more control of WebSocket frame length and can be adjusted to your application requirements. It may also reduce denial of service attacks using long data frames.