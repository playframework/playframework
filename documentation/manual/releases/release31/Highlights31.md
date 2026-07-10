<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# What's new in Play 3.1 (maybe 4.0)

This section highlights the new features of Play 3.1. If you want to learn about the changes you need to make when you migrate to Play 3.1, check out the [[Play 3.1 Migration Guide|Migration31]].

## Other Additions

### RFC 7239 Remote Identities

Play now exposes the selected remote identity through `RequestHeader.connection.remoteNode` in Scala, and `Http.RequestHeader.connection().remoteNode()` in Java. Applications that need the selected identity as a string can use `RequestHeader.connection.remoteIdentity` in Scala, or `Http.RequestHeader.connection().remoteIdentity()` in Java. `RequestHeader.remoteIdentity` and `Http.RequestHeader.remoteIdentity()` are available as request-level shortcuts.

This supports [RFC 7239](https://tools.ietf.org/html/rfc7239) `Forwarded` identifiers that are not IP addresses, such as `for=unknown` and obfuscated identifiers like `for=_hidden`. The existing `remoteAddress` APIs are deprecated because they cannot represent these identifiers and may return a fallback proxy address when the selected forwarded identity is not an IP address.

For RFC 7239 `Forwarded` headers, Play also exposes the selected element's `by` parameter through `RequestHeader.connection.byNode` in Scala, and `Http.RequestHeader.connection().byNode()` in Java. This identifies the proxy interface that received the request represented by `remoteNode`.

Play can also use the selected trusted RFC 7239 `host` parameter for `RequestHeader.host`, allowing applications behind trusted proxies to reconstruct the original public host from standards-based forwarding information. This behavior is disabled by default and can be enabled with `play.http.forwarded.trustForwardedHost = true`.

Known RFC 7239 obfuscated proxy identifiers can also be trusted explicitly:

```hocon
play.http.forwarded.trustedProxyIdentifiers = ["_edge"]
```

This allows Play to continue scanning through configured obfuscated proxy identifiers. The setting only applies to RFC 7239 `Forwarded` headers and does not make the `unknown` identifier trusted.

Play also validates RFC 7239 field syntax, including tokens, quoted strings, quoted-pair escapes, HTTP lists, and duplicate parameters. Malformed fields stop trusted-proxy scanning at the last verified connection instead of being skipped. Existing support for unquoted `for` node values containing IPv6 addresses or ports remains available for Play 3.0 compatibility. Play applies the same allowance to `by` values for consistent node parsing, although proxies should emit the quoted RFC syntax.

### Remote Connection Port

Play now exposes the remote connection port, when known, through `RequestHeader.connection.remotePort` in Scala, and `Http.RequestHeader.connection().remotePort()` in Java. `RequestHeader.remotePort` and `Http.RequestHeader.remotePort()` are available as request-level shortcuts.

The Netty and Pekko HTTP server backends populate this value from the raw socket connection. Trusted forwarded headers can also provide this value through RFC 7239 `Forwarded` ports or `X-Forwarded-Port`.

### Trusting Single X-Forwarded-Proto Values

Play can now be configured to trust a single `X-Forwarded-Proto` value when `X-Forwarded-For` contains multiple addresses:

```hocon
play.http.forwarded.trustSingleXForwardedProto = true
```

This helps deployments where trusted proxy chains append to `X-Forwarded-For`, but the edge proxy sets one `X-Forwarded-Proto` value for the original client request. The setting is disabled by default and only applies to `play.http.forwarded.version = "x-forwarded"`.

Only enable it when the trusted edge proxy overwrites or strips any incoming client-supplied `X-Forwarded-Proto` header before setting the correct value.

Play can also be configured to trust a single `X-Forwarded-Proto` value when `X-Forwarded-For` is absent:

```hocon
play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor = true
```

This setting updates the secure flag for the trusted proxy connection, but does not change the selected remote identity.

### WebSocket Compression

The Play Pekko HTTP and Netty server backends now support WebSocket compression using the RFC 7692 `permessage-deflate` extension.

Compression is enabled by default and is negotiated during the WebSocket handshake when the client offers `permessage-deflate` in the `Sec-WebSocket-Extensions` header. Applications can disable WebSocket compression for all server backends with:

```hocon
play.server.websocket.compression.enabled = false
```

Common tuning options are available under `play.server.websocket.compression`, including the compression level, preferred client window size, context-takeover behavior, and the decompression allocation limit. By default, the allocation limit follows `play.server.websocket.frame.maxLength`. The Netty backend also exposes Netty-specific settings under `play.server.netty.websocket.compression.perMessageDeflate`, including `allowServerWindowSize`, `serverWindowSize`, and `memLevel`.

For more details, see the [[Scala WebSocket documentation|ScalaWebSockets#Configuring-WebSocket-compression]] and [[Java WebSocket documentation|JavaWebSockets#Configuring-WebSocket-compression]].

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
