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

### Request metadata and forwarded-header changes

Play replaces the legacy combined connection APIs with typed `remote`, `transport`, `scheme`, and `authority` metadata. Applications may need to update request accessors and test builders, IP filter entries, custom request implementations, and proxy configuration.

Forwarded-header parsing, Host and absolute-target validation, scheme retention, CORS origin checks, and Redirect HTTPS behavior have also changed. See [[Request metadata and forwarded-header migration|RequestMetadataMigration31]] for the complete migration instructions and [[Typed request and forwarded metadata|RequestMetadataHighlights31]] for a conceptual overview.

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

### Server-initiated WebSocket closing handshakes now have a timeout

Previously, after Play sent a WebSocket Close frame, it could keep the underlying connection open until the peer replied with its own Close frame or the transport was terminated by another mechanism, such as the HTTP idle timeout. Because traffic such as Ping and Pong frames could keep the connection active, an uncooperative peer could prevent the idle timeout from terminating it.

Play now terminates the connection if the peer does not acknowledge the Close frame within three seconds. The closing-handshake deadline starts when Play emits the Close frame, is not extended by subsequent traffic, and suppresses Play's periodic WebSocket keep-alive frames while closing. Applications that require more time can configure a different positive finite duration:

```hocon
play.server.websocket.closeTimeout = 3 seconds
```

This setting applies to both the Netty and Pekko HTTP server backends and is independent of the HTTP idle timeout.

Graceful server shutdown now sends status `1001` (Going Away) to every open WebSocket and waits up to this timeout from shutdown initiation for the peer's Close acknowledgement before terminating the connection. This overall bound also applies to a connection that is too backpressured to emit its Close frame. The framework-generated `1001` is sent only to the peer; the application's input stream completes normally without receiving it as a `CloseMessage`. The wait is part of Pekko Coordinated Shutdown's `service-requests-done` phase.

### Malformed WebSocket frames are rejected more strictly

Play now validates incoming WebSocket text and control frames consistently across the Netty and Pekko HTTP server backends.

Previously, malformed UTF-8 in a text message could be decoded with replacement characters and delivered to the application. Play now rejects the message, sends Close status `1007` to the remote peer, and does not deliver the malformed message. Valid UTF-8 remains supported when a multi-byte character is split across WebSocket fragments. This follows [RFC 6455 section 8.1](https://datatracker.ietf.org/doc/html/rfc6455#section-8.1).

Play also rejects fragmented Ping, Pong, and Close control frames, and control frames with payloads larger than 125 bytes, by sending Close status `1002`. Valid control frames can still be interleaved between fragments of a data message, as required by [RFC 6455 section 5.4](https://datatracker.ietf.org/doc/html/rfc6455#section-5.4) and [section 5.5](https://datatracker.ietf.org/doc/html/rfc6455#section-5.5).

When Netty detects and closes one of these protocol violations before it reaches Play's common WebSocket handler, Play now completes the application stream without also reporting a synthetic `1006` close message. Status `1006` remains the application-visible result for abnormal transport loss where no protocol error was already handled.

Applications that previously expected replacement characters or a synthetic `1006` for malformed frames should update their handling. Clients that send valid RFC 6455 frames are unaffected.

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
| Remote peer sends a reserved or invalid close status code, such as `1006`, `2000`, or `5000` | Backend behavior differed; the status could be delivered to application code or echoed to the peer. | Play rejects the frame and does not deliver it to the application. Depending on which backend detects the violation, it either sends status `1002` before closing or terminates the transport immediately. |
| Remote peer sends a Close reason containing malformed UTF-8 | The Pekko HTTP backend could decode the reason with replacement characters and deliver it to application code. | Play rejects the frame and does not deliver it to the application. Depending on which backend detects the violation, it either sends status `1007` before closing or terminates the transport immediately. |
| Remote peer sends a one-byte Close payload | Backend behavior differed; the Pekko HTTP backend could expose a synthetic `1002` as though the peer had sent it. | Play rejects the frame with status `1002` and does not deliver it to the application. |
| Application code sends `CloseMessage(Some(1005), "")` | Play could pass `1005` toward the backend as a status code. | Play sends an empty Close frame, represented as `CloseMessage(None, "")`. |
| Application code sends a reserved or invalid close status code, such as `1006` or `999` | Play sent the status code unchanged. | Play still sends the status code unchanged for compatibility, but logs a warning because the value is not valid in a Close frame. |
| Application code sends a Close reason that cannot be encoded as UTF-8 | Play could pass the reason toward the backend unchanged. | Play logs a warning and drops the reason. |
| Application code sends a Close reason longer than 123 UTF-8 bytes | Play could pass an invalid oversized Close frame toward the backend. | Play logs a warning and truncates the reason to the longest valid UTF-8 prefix that fits in 123 bytes. |
| Application code sends `CloseMessage(None, "reason")` | The close reason had no valid wire encoding because Close reasons require a status code. | Play logs a warning and sends `CloseMessage(None, "")`, dropping the reason. |

When Play can send an error Close in response to a malformed peer Close frame, it terminates the connection after
sending the error and does not wait for another Close acknowledgement.

Applications that create raw `CloseMessage` values should avoid sending reserved status codes such as `1005`, `1006`, and `1015`, and should keep Close reasons short enough to fit in 123 UTF-8 bytes.
