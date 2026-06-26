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

### WebSocket Handshake Headers, Cookies, and Sessions

`WebSocket.Accepted` now supports adding custom headers, cookies, and session data to the successful `101 Switching Protocols` response:

Scala
: ```scala
WebSocket.acceptWithOptions[String, String] { request =>
  WebSocket
    .Accepted(flow)
    .withHeaders("X-WebSocket-Trace" -> request.id.toString)
    .withCookies(Cookie("ws-session", "connected", httpOnly = true))
    .withSession(request.session + ("websocket" -> "connected"))
}
```

Java
: ```java
WebSocket.Text.acceptWithOptions(request ->
  new WebSocket.Accepted<>(flow)
    .withHeader("X-WebSocket-Trace", request.id().toString())
    .withCookies(Cookie.builder("ws-session", "connected").withHttpOnly(true).build())
    .addingToSession(request, "websocket", "connected"));
```

This is useful for applications that need to attach handshake metadata, for example trace identifiers, cookies, or session updates, while still using Play's WebSocket handling. These headers, cookies, and session updates are sent only with the opening handshake response. Protocol-owned headers such as `Upgrade`, `Connection`, `Sec-WebSocket-Accept`, and `Sec-WebSocket-Protocol` remain controlled by Play and the selected `subprotocol`.

For more details, see the [[Scala WebSocket documentation|ScalaWebSockets#Setting-WebSocket-handshake-headers-and-cookies]] and [[Java WebSocket documentation|JavaWebSockets#Setting-WebSocket-handshake-headers-and-cookies]].
