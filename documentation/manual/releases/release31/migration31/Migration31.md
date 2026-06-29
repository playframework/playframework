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
