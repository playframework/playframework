<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# What's new in Play 3.1 (maybe 4.0)

This section highlights the new features of Play 3.1. If you want to learn about the changes you need to make when you migrate to Play 3.1, check out the [[Play 3.1 Migration Guide|Migration31]].

## Other Additions

### WebSocket Subprotocol Selection

Play now lets applications explicitly select the WebSocket subprotocol that is sent back in the successful `101 Switching Protocols` response.

Previously, WebSocket handlers could inspect the incoming `Sec-WebSocket-Protocol` request header and decide whether to accept or reject the connection, but the accepted `WebSocket` only returned a flow. This meant the server backend decided which subprotocol to announce, which made it difficult to support clients that offer multiple protocols, such as:

```http
Sec-WebSocket-Protocol: graphql-ws, graphql-transport-ws
```

Applications can now return a `WebSocket.Accepted` value from the new `acceptWithOptions` or `acceptOrResultWithOptions` APIs and include the selected subprotocol:

Scala
: ```scala
WebSocket.acceptWithOptions[String, String] { request =>
  WebSocket.Accepted(flow, subprotocol = Some("graphql-transport-ws"))
}
```

Java
: ```java
WebSocket.Text.acceptWithOptions(request ->
  new WebSocket.Accepted<>(flow, "graphql-transport-ws"));
```

This is useful for protocols where the client and server need to agree on an application-level WebSocket protocol during the opening handshake. Existing `accept` and `acceptOrResult` handlers keep their previous behavior.

For more details, see the [[Scala WebSocket documentation|ScalaWebSockets#Selecting-a-WebSocket-subprotocol]] and [[Java WebSocket documentation|JavaWebSockets#Selecting-a-WebSocket-subprotocol]].

### WebSocket Abnormal Closure Status

Play now reports abnormal WebSocket connection loss to raw WebSocket handlers with close status `1006`.

Previously, if the underlying connection disappeared without a WebSocket Close frame, for example because the network connection was lost or an idle timeout closed the transport, raw `WebSocket.accept[Message, Message]` handlers could see the stream complete without a close message. Play now emits a `CloseMessage` with status code `1006` to application code before completing the stream.

The `1006` status is never sent to the remote peer as a WebSocket Close frame. It is only used as the application-visible status for a connection that closed abnormally without receiving a Close frame. This behavior follows [RFC 6455 section 7.1.5](https://datatracker.ietf.org/doc/html/rfc6455#section-7.1.5) and the reserved close code definition in [section 7.4.1](https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1).
