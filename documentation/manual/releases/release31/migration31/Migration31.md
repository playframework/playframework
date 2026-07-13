<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Play 3.1 Migration Guide

TBD

## How to migrate

Before starting `sbt`, make sure to make the following upgrades.

### Play upgrade

TBD

### sbt upgrade

TBD

### Minimum required Java and sbt version

TBD

### Play upgraded to Pekko 2 and Pekko HTTP 2

Play now uses Pekko 2 and Pekko HTTP 2. If your build overrides Play's Pekko dependencies, align those overrides with the Pekko 2 and Pekko HTTP 2 versions used by this Play release and review the upstream Pekko migration notes for any APIs you use directly.

The deprecated low-level `org.apache.pekko.http.play.WebSocketHandler.handleWebSocket` overloads that accepted Pekko HTTP's old `UpgradeToWebSocket` API have been removed. Code using this internal Pekko HTTP bridge should use the maintained `WebSocketUpgrade` overload instead.

### Remote address APIs deprecated in favor of remote node APIs

Play now supports RFC 7239 `Forwarded` remote identifiers that are not IP addresses, such as `for=unknown` and obfuscated identifiers like `for=_hidden`. The old `remoteAddress` APIs cannot represent these values, so they are deprecated:

* Scala `RequestHeader.remoteAddress`
* Scala `RemoteConnection.remoteAddress`
* Scala `RemoteConnection.remoteAddressString`
* Java `Http.RequestHeader.remoteAddress()`

Use `RequestHeader.connection.remoteNode` when you need the structured remote identity selected from trusted forwarded headers. Use `RequestHeader.connection.remoteIdentity` when you need that identity as a string, or `RequestHeader.connection.remoteIpAddress` when you specifically need an IP address. `RequestHeader.remoteIdentity` is available as a request-level shortcut.

For compatibility, the deprecated `remoteAddress` APIs still return an IP address. If the selected RFC 7239 remote identity is `unknown` or obfuscated, that IP address is only a fallback, usually the previous trusted proxy address, and does not represent the actual RFC 7239 remote identity.

For example, with a trusted direct proxy at `127.0.0.1`:

```http
Forwarded: for=_hidden;proto=https
```

Play reports:

```scala
request.connection.remoteNode      // RemoteNode.Obfuscated("_hidden", None)
request.connection.remoteIdentity  // "_hidden"
request.connection.remoteIpAddress // None
request.secure                     // true
```

The deprecated `request.remoteAddress` method still returns the fallback legacy value, such as
`"127.0.0.1"` in this example.

### RFC 7239 Forwarded header syntax is validated

Play now validates RFC 7239 `Forwarded` field values before using them. Parameter names must be valid tokens, values must be tokens or quoted strings, and a parameter cannot occur more than once in one forwarded element. Empty HTTP list elements remain accepted and are ignored.

RFC 7239 requires IPv6 addresses and node identifiers with ports to be quoted because they contain `:`:

```http
Forwarded: for="[2001:db8:cafe::17]:4711"
Forwarded: for="192.0.2.43:4711"
```

For compatibility with Play 3.0, Play continues to accept these values without quotes in the `for` parameter. Play applies the same allowance to `by` for consistent node parsing. Proxy configurations should nevertheless be updated to emit the quoted RFC syntax. Other parameters do not receive this compatibility allowance.

When Play encounters a malformed `Forwarded` field while scanning a trusted proxy chain, it stops at that field and keeps the last verified connection information. Check that each trusted proxy emits valid RFC 7239 syntax before upgrading.

### Redirect HTTPS filter only reads X-Forwarded-Proto when enabled

The Redirect HTTPS filter previously treated `X-Forwarded-Proto: https` as a secure request even when `play.filters.https.xForwardedProtoEnabled` was `false`. The filter now ignores `X-Forwarded-Proto` unless that legacy option is explicitly enabled. Deployments that relied on this implicit header handling may otherwise repeatedly redirect a public HTTPS request when a proxy terminates HTTPS but Play still sees the proxy connection as insecure.

Applications behind a trusted proxy should normally configure [[trusted proxies|HTTPServer#configuring-trusted-proxies]] so Play derives `request.secure` after validating the forwarded proxy chain. If a trusted proxy sends a single `X-Forwarded-Proto` value without `X-Forwarded-For`, enable:

```hocon
play.http.forwarded.trustXForwardedProtoWithoutXForwardedFor = true
```

The immediate proxy must also be included in `play.http.forwarded.trustedProxies`. Applications that intentionally rely on the filter reading `X-Forwarded-Proto` directly can instead set:

```hocon
play.filters.https.xForwardedProtoEnabled = true
```

This legacy option changes more than the secure-request check: the filter redirects only requests with `X-Forwarded-Proto: http`. Requests with a missing or unexpected value are passed to the application without an HTTPS redirect or HSTS header, and the option does not update `request.secure`. Only enable it when bypassing redirects for requests without the header is intentional, or when a trusted proxy removes or overwrites every client-supplied value and reliably sends either `http` or `https`.

### HEAD responses no longer include generated Content-Length headers

Play no longer renders generated `Content-Length` headers for `HEAD` responses. `HEAD` responses still do not include a response body, but applications and tests should not rely on `Content-Length` being present on a `HEAD` response, even when the equivalent `GET` response has a known length.

This behavior follows Pekko HTTP 2, which changed generated `Content-Length` rendering in [apache/pekko-http#962](https://github.com/apache/pekko-http/pull/962), ported from [akka/akka-http#4214](https://github.com/akka/akka-http/pull/4214). The original upstream change fixed response framing for statuses such as `205 Reset Content` and made `Content-Length` rendering depend on the request method and response status.

If your tests compare `HEAD` and `GET` response headers, exclude `Content-Length` from that comparison. If your application needs to expose resource size metadata for `HEAD` requests, use an application-specific header.

### Clustered Pekko applications may require additional JVM add-opens

Applications that configure the Play ActorSystem as a Pekko cluster, including applications using Play's cluster-sharding module, start Pekko Remote/Artery TCP. Artery TCP depends on Agrona, which accesses `jdk.internal.misc.Unsafe` on the JVM.

On strongly encapsulated JDKs this can require the following JVM option:

```text
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
```

Add this option to the JVM that runs the application or tests when using clustered Pekko/Artery TCP and you see an access failure involving `org.agrona.UnsafeApi` and `jdk.internal.misc.Unsafe`.

### Java form binding no longer depends on Spring Framework libraries

Historically, Play's Java form binding used Spring Framework libraries, going back to the beginning of Play 2. Starting with this release, Play owns the form binding code it needs internally and registers the supported default conversions through Play's `Formatters` infrastructure.

This removes Spring from the application classpath for Java form binding and gives Play direct control over the binding behavior. The old integration inherited behavior that was useful for Spring bean configuration, but surprising for Play web forms: classpath scanning and factory lookup during JavaBean introspection, convention-based converter class loading (so called "editors" in Spring), resource location handling, class loading, and default converters that could resolve files, URLs, classpath resources, streams, or readers from submitted form values.

For Play applications, form binding should convert submitted request strings into application data values. It should not, by default, interpret user input as Spring resource expressions, open resources, inspect the classpath, or load classes. This avoids surprising resource access from user-submitted form data, such as opening streams or readers, resolving classpath resources, or loading classes during form binding.

As a result, the following types and Spring-specific behaviors are no longer bound by default:

* `java.io.File`
* `java.nio.file.Path`
* `java.io.InputStream`
* `java.io.Reader`
* `org.xml.sax.InputSource`
* `java.lang.Class`
* `java.lang.Class[]`
* Raw `java.lang.Enum` targets. Concrete enum types continue to bind by enum constant name.
* Spring resource types and resource patterns, if Spring is present in the application
* Spring-style resource locations such as `classpath:` URL/resource binding

Plain `URI` values are still parsed as URI values. For example, a `classpath:` URI is treated as ordinary URI text and is not resolved as a classpath resource by Play.

`URL` values are parsed as regular URLs only. Spring-style `classpath:` URL/resource binding is not supported.

This does not affect normal file uploads. Play file uploads use multipart form handling and `Http.MultipartFormData.FilePart`, not string-to-`File` form binding. See [[Handling file upload|JavaFileUpload]] for the Java file upload API.

If your application intentionally needs one of the removed bindings, register an explicit formatter or converter for that type in your application. See [[Register a custom DataBinder|JavaForms#Register-a-custom-DataBinder]] for the Java form formatter setup. If you think a removed binding should be supported by Play by default, please open an issue in the [Play issue tracker](https://github.com/playframework/playframework/issues).

### Raw WebSocket handlers now receive status 1006 for abnormal connection loss

Raw WebSocket handlers that consume `play.api.http.websocket.Message` values now receive `CloseMessage(Some(1006), ...)` when the underlying connection closes or fails without Play receiving a WebSocket Close frame.

This can happen when the network connection is interrupted, the client disconnects without completing the WebSocket close handshake, or the server idle timeout closes the transport. The status code `1006` is not sent on the wire; it is only delivered to application code to report that the connection was closed abnormally. This is the behavior defined by [RFC 6455 section 7.1.5](https://datatracker.ietf.org/doc/html/rfc6455#section-7.1.5) and the reserved close code definition in [section 7.4.1](https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1).

Because abnormal WebSocket termination is now reported as a `CloseMessage` before the stream completes, raw `Message` handlers that previously relied on `watchTermination` seeing a failed stream for transport loss should instead inspect `CloseMessage(Some(1006), ...)`.

Scala
: @[abnormal-closure](code/WebSocketCloseMigration.scala)

Java
: @[abnormal-closure](code/WebSocketCloseMigration.java)

Handlers using typed APIs such as `WebSocket.accept[String, String]` still do not receive close control frames as typed messages. Use a raw `Message` flow if your application needs to inspect WebSocket close status codes directly.

### WebSocket close messages from typed transformers and application failures are more consistent

Play now preserves more application-level WebSocket close reasons as WebSocket Close frames instead of turning them into generic stream termination.

For Scala WebSockets, if an application source failed with `play.api.http.websocket.WebSocketCloseException`, the close status carried by the exception was not preserved reliably. This could be handled as a generic application stream failure instead of closing the WebSocket with the supplied status. Play now closes the connection with the exception's embedded `CloseMessage`.

Scala
: @[websocket-close-exception](code/WebSocketCloseMigration.scala)

Scala high-level JSON WebSockets created with `WebSocket.MessageFlowTransformer.jsonMessageFlowTransformer[In, Out]` now close with status `1003` when incoming JSON is syntactically valid but fails the configured `Reads[In]` validation. The invalid message is still not delivered to the typed application flow, but the remote peer now receives the intended `1003` close status with the validation error reason.

Scala
: @[typed-json-validation](code/WebSocketCloseMigration.scala)

Java typed JSON WebSockets created with `play.mvc.WebSocket.json(Class)` now use a bounded generic close reason for JSON decoding failures. The close status remains `1003`, but the reason is now `"Unable to parse JSON message"` instead of the underlying Jackson exception message.

Java
: @[typed-json-decoding](code/WebSocketCloseMigration.java)

This avoids creating invalid WebSocket Close frames: [RFC 6455 section 5.5](https://datatracker.ietf.org/doc/html/rfc6455#section-5.5) limits all control frame payloads, including Close frames, to 125 bytes, and [section 5.5.1](https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.1) defines the first two bytes of a Close frame body as the status code, leaving at most 123 bytes for the UTF-8 reason.

### WebSocket Close frame handling is more RFC-compliant

Play now normalizes additional WebSocket Close frame edge cases in the common WebSocket flow handler. These changes keep application-visible close status reporting compatible where possible, while avoiding invalid Close frames on the wire.

[RFC 6455 section 5.5](https://datatracker.ietf.org/doc/html/rfc6455#section-5.5) limits WebSocket control frames to 125 bytes. [Section 5.5.1](https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.1) defines Close frame payloads as an optional 2-byte status code followed by an optional UTF-8 reason. [Section 7.4.1](https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.1) defines status codes such as `1005`, `1006`, and `1015` as reserved values that must not be sent as status codes in a Close control frame.

| **Case** | **Previous behavior** | **New behavior** |
| --- | --- | --- |
| Play echoes an empty Close frame from the remote peer | Play could represent the echoed frame as `CloseMessage(Some(1005), "")`. | Play sends an empty Close frame, represented as `CloseMessage(None, "")`, so `1005` is not sent on the wire. |
| Application code sends `CloseMessage(Some(1005), "")` | Play could pass `1005` toward the backend as a status code. | Play sends an empty Close frame, represented as `CloseMessage(None, "")`. |
| Application code sends a reserved or invalid close status code, such as `1006` or `999` | Play sent the status code unchanged. | Play still sends the status code unchanged for compatibility, but logs a warning because the value is not valid in a Close frame. |
| Application code sends a Close reason that cannot be encoded as UTF-8 | Play could pass the reason toward the backend unchanged. | Play logs a warning and drops the reason. |
| Application code sends a Close reason longer than 123 UTF-8 bytes | Play could pass an invalid oversized Close frame toward the backend. | Play logs a warning and truncates the reason to the longest valid UTF-8 prefix that fits in 123 bytes. |
| Application code sends `CloseMessage(None, "reason")` | The close reason had no valid wire encoding because Close reasons require a status code. | Play logs a warning and sends `CloseMessage(None, "")`, dropping the reason. |

Applications that create raw `CloseMessage` values should avoid sending reserved status codes such as `1005`, `1006`, and `1015`, and should keep Close reasons short enough to fit in 123 UTF-8 bytes.
