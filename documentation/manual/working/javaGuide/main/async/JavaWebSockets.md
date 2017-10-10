<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# WebSockets

[WebSockets](https://en.wikipedia.org/wiki/WebSocket) are sockets that can be used from a web browser based on a protocol that allows two way full duplex communication.  The client can send messages and the server can receive messages at any time, as long as there is an active WebSocket connection between the server and the client.

Modern HTML5 compliant web browsers natively support WebSockets via a JavaScript WebSocket API.  However WebSockets are not limited in just being used by WebBrowsers, there are many WebSocket client libraries available, allowing for example servers to talk to each other, and also native mobile apps to use WebSockets.  Using WebSockets in these contexts has the advantage of being able to reuse the existing TCP port that a Play server uses.

> **Tip:** Check [caniuse.com](http://caniuse.com/#feat=websockets) to see more about which browsers supports WebSockets, known issues and more information.

## Handling WebSockets

Until now, we've been writing methods that return `Result` to handle standard HTTP requests.  WebSockets are quite different and can’t be handled via standard Play actions.

Play provides two different built in mechanisms for handling WebSockets.  The first is using actors, the second is using simple callbacks.  Both of these mechanisms can be accessed using the builders provided on [WebSocket](api/java/play/mvc/WebSocket.html).

## Handling WebSockets with actors

To handle a WebSocket with an actor, we need to give Play a `akka.actor.Props` object that describes the actor that Play should create when it receives the WebSocket connection.  Play will give us an `akka.actor.ActorRef` to send upstream messages to, so we can use that to help create the `Props` object:

@[imports](code/javaguide/async/JavaWebSockets.java)
@[actor-accept](code/javaguide/async/JavaWebSockets.java)

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

Sometimes you may wish to reject a WebSocket request, for example, if the user must be authenticated to connect to the WebSocket, or if the WebSocket is associated with some resource, whose id is passed in the path, but no resource with that id exists.  Play provides a `reject` WebSocket builder for this purpose:

@[actor-reject] @Deprecated(code/javaguide/async/JavaWebSockets.java)

> **Note**: the WebSocket protocol does not implement [Same Origin Policy](https://en.wikipedia.org/wiki/Same-origin_policy), and so does not protect against [Cross-Site WebSocket Hijacking](http://www.christian-schneider.net/CrossSiteWebSocketHijacking.html).  To secure a websocket against hijacking, the `Origin` header in the request must be checked against the server's origin, and manual authentication (including CSRF tokens) should be implemented.  If a WebSocket request does not pass the security checks, then `acceptOrResult` should reject the request by returning a Forbidden result.

### Accepting a WebSocket asynchronously

You may need to do some asynchronous processing before you are ready to create an actor or reject the WebSocket, if that's the case, you can simply return `CompletionStage<WebSocket<A>>` instead of `WebSocket<A>`.

### Handling different types of messages

So far we have only seen handling `String` frames.  Play also has built in handlers for `byte[]` frames, and `JSONNode` messages parsed from `String` frames.  You can pass these as the type parameters to the WebSocket creation method, for example:

@[actor-json](code/javaguide/async/JavaWebSockets.java)

## Handling WebSockets using callbacks

If you don't want to use actors to handle a WebSocket, you can also handle it using simple callbacks.

To handle a WebSocket your method must return a `WebSocket` instead of a `Result`:

@[websocket](code/javaguide/async/JavaWebSockets.java)

A WebSocket has access to the request headers (from the HTTP request that initiates the WebSocket connection) allowing you to retrieve standard headers and session data. But it doesn't have access to any request body, nor to the HTTP response.

When the `WebSocket` is ready, you get both `in` and `out` channels.

It this example, we print each message to console and we send a single **Hello!** message.

> **Tip:** You can test your WebSocket controller on <https://www.websocket.org/echo.html>. Just set the location to `ws://localhost:9000`.

Let’s write another example that totally discards the input data and closes the socket just after sending the **Hello!** message:

@[discard-input](code/javaguide/async/JavaWebSockets.java)

## Configuring WebSocket Frame Length

You can configure the max length for [WebSocket data frames](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers#Format) using `play.websocket.buffer.limit` or passing `-Dwebsocket.buffer.limit` system property when running your application. For example:

```
sbt -Dwebsocket.buffer.limit=64k run
```

This configuration gives you more control of WebSocket frame length and can be adjusted to your application requirements. It may also reduce denial of service attacks using long data frames.
