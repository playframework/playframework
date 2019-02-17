<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Calling REST APIs with Play WS

Sometimes we would like to call other HTTP services from within a Play application. Play supports this via its [WS library](api/java/play/libs/ws/package-summary.html), which provides a way to make asynchronous HTTP calls.

There are two important parts to using the WS API: making a request, and processing the response. We'll discuss how to make both GET and POST HTTP requests first, and then show how to process the response from the WS library. Finally, we'll discuss some common use cases.

> **Note**: In Play 2.6, Play WS has been split into two, with an underlying standalone client that does not depend on Play, and a wrapper on top that uses Play specific classes.  In addition, shaded versions of AsyncHttpClient and Netty are now used in Play WS to minimize library conflicts, primarily so that Play's HTTP engine can use a different version of Netty.  Please see the [[2.6 migration guide|WSMigration26]] for more information.

## Adding WS to project

To use WS, first add `javaWs` to your `build.sbt` file:

@[javaws-sbt-dependencies](code/javaws.sbt)


## Enabling HTTP Caching in Play WS

Play WS supports [HTTP caching](https://tools.ietf.org/html/rfc7234), but requires a JSR-107 cache implementation to enable this feature.  You can add `ehcache`:

```scala
libraryDependencies += ehcache
```

Or you can use another JSR-107 compatible cache such as [Caffeine](https://github.com/ben-manes/caffeine/wiki/JCache).

Once you have the library dependencies, then enable the HTTP cache as shown on [[WS Cache Configuration|WsCache]] page.

Using an HTTP cache means savings on repeated requests to backend REST services, and is especially useful when combined with resiliency features such as [`stale-on-error` and `stale-while-revalidate`](https://tools.ietf.org/html/rfc5861).

## Making a Request

Now any controller or component that wants to use WS will have to add the following imports and then declare a dependency on the [`WSClient`](api/java/play/libs/ws/WSClient.html) type to use dependency injection:

@[ws-controller](code/javaguide/ws/MyClient.java)

To build an HTTP request, you start with `ws.url()` to specify the URL.

@[ws-holder](code/javaguide/ws/JavaWS.java)

This returns a [`WSRequest`](api/java/play/libs/ws/WSRequest.html) that you can use to specify various HTTP options, such as setting headers. You can chain calls together to construct complex requests.

@[ws-complex-holder](code/javaguide/ws/JavaWS.java)

You end by calling a method corresponding to the HTTP method you want to use.  This ends the chain, and uses all the options defined on the built request in the [`WSRequest`](api/java/play/libs/ws/WSRequest.html).

@[ws-get](code/javaguide/ws/JavaWS.java)

This returns a [`CompletionStage<WSResponse>`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) where the [`WSResponse`](api/java/play/libs/ws/WSResponse.html) contains the data returned from the server.

> Java 1.8 uses [`CompletionStage`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) to manage asynchronous code, and Java WS API relies heavily on composing `CompletionStage` together with different methods.  If you have been using an earlier version of Play that used `F.Promise`, then the [CompletionStage section of the migration guide](https://www.playframework.com/documentation/2.5.x/JavaMigration25#Replaced-F.Promise-with-Java-8s-CompletionStage) will be very helpful.

> If you are doing any blocking work, including any kind of DNS work such as calling [`java.util.URL.equals()`](https://docs.oracle.com/javase/8/docs/api/java/net/URL.html#equals-java.lang.Object-), then you should use a custom execution context as described in [[ThreadPools]], preferably through a [`CustomExecutionContext`](api/java/play/libs/concurrent/CustomExecutionContext.html).  You should size the pool to leave a safety margin large enough to account for failures. 

> If you are calling out to an [unreliable network](https://queue.acm.org/detail.cfm?id=2655736), consider using [`Futures.timeout`](api/java/play/libs/concurrent/Futures.html) and a  [circuit breaker](https://martinfowler.com/bliki/CircuitBreaker.html) like [Failsafe](https://github.com/jhalterman/failsafe#circuit-breakers).

### Request with authentication

If you need to use HTTP authentication, you can specify it in the builder, using a username, password, and an `WSAuthScheme`.  Options for the `WSAuthScheme` are `BASIC`, `DIGEST`, `KERBEROS`, `NTLM`, and `SPNEGO`.

@[ws-auth](code/javaguide/ws/JavaWS.java)

### Request with follow redirects

If an HTTP call results in a 302 or a 301 redirect, you can automatically follow the redirect without having to make another call.

@[ws-follow-redirects](code/javaguide/ws/JavaWS.java)

### Request with query parameters

You can specify query parameters for a request.

@[ws-query-parameter](code/javaguide/ws/JavaWS.java)

### Request with additional headers

@[ws-header](code/javaguide/ws/JavaWS.java)

For example, if you are sending plain text in a particular format, you may want to define the content type explicitly.

@[ws-header-content-type](code/javaguide/ws/JavaWS.java)

### Request with cookie

You can specify cookies for a request, using `WSCookieBuilder`:

@[ws-cookie](code/javaguide/ws/JavaWS.java)

### Request with timeout

If you wish to specify a request timeout, you can use `setRequestTimeout` to set a value in milliseconds. A value of `Duration.ofMillis(Long.MAX_VALUE)` can be used to set an infinite timeout.

@[ws-timeout](code/javaguide/ws/JavaWS.java)

### Submitting form data

To post url-form-encoded data you can set the proper header and formatted data with a content type of "application/x-www-form-urlencoded".

@[ws-post-form-data](code/javaguide/ws/JavaWS.java)

### Submitting multipart/form data

The easiest way to post multipart/form data is to use a `Source<Http.MultipartFormData.Part<Source<ByteString>, ?>, ?>`:

@[multipart-imports](code/javaguide/ws/JavaWS.java)

@[ws-post-multipart](code/javaguide/ws/JavaWS.java)

To upload a File as part of multipart form data, you need to pass a `Http.MultipartFormData.FilePart<Source<ByteString>, ?>` to the `Source`:

@[ws-post-multipart2](code/javaguide/ws/JavaWS.java)

### Submitting JSON data

The easiest way to post JSON data is to use Play's JSON support, using `play.libs.Json`:

@[json-imports](code/javaguide/ws/JavaWS.java)

@[ws-post-json](code/javaguide/ws/JavaWS.java)

You can also pass in a custom `ObjectMapper`:

@[ws-post-json-objectmapper](code/javaguide/ws/JavaWS.java)

### Submitting XML data

The easiest way to post XML data is to use Play's XML support, using [`play.libs.XML`](api/java/play/libs/XML.html):

@[ws-post-xml](code/javaguide/ws/JavaWS.java)

### Submitting Streaming data

It's also possible to stream data in the request body using [Akka Streams](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html?language=java).

Here is an example showing how you could stream a large image to a different endpoint for further processing:

@[ws-stream-request](code/javaguide/ws/JavaWS.java)

The `largeImage` in the code snippet above is a `Source<ByteString, ?>`.

### Request Filters

You can do additional processing on a [`WSRequest`](api/java/play/libs/ws/WSRequest.html) by adding a request filter.  A request filter is added by extending the `play.libs.ws.WSRequestFilter` trait, and then adding it to the request with [`request.withRequestFilter(filter)`](api/java/play/libs/ws/WSRequest.html#setRequestFilter-play.libs.ws.WSRequestFilter-).

@[ws-request-filter](code/javaguide/ws/JavaWS.java)

## Processing the Response

Working with the [`WSResponse`](api/java/play/libs/ws/WSResponse.html) is done by applying transformations such as `thenApply` and `thenCompose` to the `CompletionStage`.

### Processing a response as JSON

You can process the response as a `JsonNode` by calling `r.getBody(json())`, using the default method from `play.libs.ws.WSBodyReadables.json()`.

@[ws-response-json](code/javaguide/ws/JavaWS.java)

### Processing a response as XML

Similarly, you can process the response as XML by calling `r.getBody(xml())`, using the default method from `play.libs.ws.WSBodyReadables.xml()`.

@[ws-response-xml](code/javaguide/ws/JavaWS.java)

### Processing large responses

Calling `get()`, `post()` or `execute()` will cause the body of the response to be loaded into memory before the response is made available.  When you are downloading a large, multi-gigabyte file, this may result in unwelcome garbage collection or even out of memory errors.

You can consume the response's body incrementally by using an [Akka Streams](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html?language=java) `Sink`.  The [`stream()`](api/java/play/libs/ws/WSRequest.html#stream--) method on `WSRequest` returns a `CompletionStage<WSResponse>`, where the `WSResponse` contains a [`getBodyAsStream()`](api/java/play/libs/ws/WSResponse.html#getBodyAsStream--) method that provides a `Source<ByteString, ?>`.

> **Note**: In 2.5.x, a `StreamedResponse` was returned in response to a [`request.stream()`](api/java/play/libs/ws/WSRequest.html#stream--) call.  In 2.6.x, a standard [`WSResponse`](api/java/play/libs/ws/WSResponse.html) is returned, and the `getBodyAsSource()` method should be used to return the Source.

Any controller or component that wants to leverage the WS streaming functionality will have to add the following imports and dependencies:

@[ws-streams-controller](code/javaguide/ws/MyController.java)

Here is a trivial example that uses a folding `Sink` to count the number of bytes returned by the response:

@[stream-count-bytes](code/javaguide/ws/JavaWS.java)

Alternatively, you could also stream the body out to another location. For example, a file:

@[stream-to-file](code/javaguide/ws/JavaWS.java)

Another common destination for response bodies is to stream them back from a controller's `Action`:

@[stream-to-result](code/javaguide/ws/JavaWS.java)

As you may have noticed, before calling [`stream()`](api/java/play/libs/ws/WSRequest.html#stream--) we need to set the HTTP method to use by calling [`setMethod(String)`](api/java/play/libs/ws/WSRequest.html#setMethod-java.lang.String-) on the request. Here follows another example that uses `PUT` instead of `GET`:

@[stream-put](code/javaguide/ws/JavaWS.java)

Of course, you can use any other valid HTTP verb.

## Common Patterns and Use Cases

### Chaining WS calls

You can chain WS calls by using [`thenCompose`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#thenCompose-java.util.function.Function-).

@[ws-composition](code/javaguide/ws/JavaWS.java)

### Exception recovery

If you want to recover from an exception in the call, you can use [`handle`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#handle-java.util.function.BiFunction-) or [`exceptionally`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#exceptionally-java.util.function.Function-) to substitute a response.

@[ws-recover](code/javaguide/ws/JavaWS.java)

### Using in a controller

You can map a `CompletionStage<WSResponse>` to a `CompletionStage<Result>` that can be handled directly by the Play server, using the asynchronous action pattern defined in [[Handling Asynchronous Results|JavaAsync]].

@[ws-action](code/javaguide/ws/JavaWS.java)

### Using WSClient with Futures Timeout

If a chain of WS calls does not complete in time, it may be useful to wrap the result in a timeout block, which will return a failed Future if the chain does not complete in time -- this is more generic than using `withRequestTimeout`, which only applies to a single request.
The best way to do this is with Play's [[non-blocking timeout feature|JavaAsync]], using [`Futures.timeout`](api/java/play/libs/concurrent/Futures.html) and [`CustomExecutionContext`](api/java/play/libs/concurrent/CustomExecutionContext.html) to ensure some kind of resolution:

@[ws-futures-timeout](code/javaguide/ws/JavaWS.java)

## Directly creating WSClient

We recommend that you get your `WSClient` instances using [[dependency injection|JavaDependencyInjection]] as described above. `WSClient` instances created through dependency injection are simpler to use because they are automatically created when the application starts and cleaned up when the application stops.

However, if you choose, you can instantiate a `WSClient` directly from code and use this for making requests or for configuring underlying `AsyncHttpClient` options.

> **Note:** If you create a `WSClient` manually then you **must** call `client.close()` to clean it up when you've finished with it. Each client creates its own thread pool. If you fail to close the client or if you create too many clients then you will run out of threads or file handles -— you'll get errors like "Unable to create new native thread" or "too many open files" as the underlying resources are consumed.

Here is an example of how to create a `WSClient` instance by yourself:

@[ws-client-imports](code/javaguide/ws/JavaWS.java)

@[ws-client](code/javaguide/ws/JavaWS.java)

You can also use [`play.test.WSTestClient.newClient`](api/java/play/test/WSTestClient.html) to create an instance of `WSClient` in a functional test.  See [[JavaTestingWebServiceClients]] for more details.

Or, you can run the `WSClient` completely standalone without involving a running Play application or configuration at all:

@[ws-standalone-imports](code/javaguide/ws/Standalone.java)

@[ws-standalone](code/javaguide/ws/Standalone.java)

If you want to run `WSClient` standalone, but still use [[configuration|JavaWS#configuring-ws]] (including [[SSL|WsSSL]]), you can use a configuration parser like this:

@[ws-standalone-with-config](code/javaguide/ws/StandaloneWithConfig.java)

Again, once you are done with your custom client work, you **must** close the client, or you will leak threads:

@[ws-close-client](code/javaguide/ws/JavaWS.java)

Ideally, you should only close a client after you know all requests have been completed.  You should not use [`try-with-resources`](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) to automatically close a WSClient instance, because WSClient logic is asynchronous and `try-with-resources` only supports synchronous code in its body.

## Custom BodyReadables and BodyWritables

Play WS comes with rich type support for bodies in the form of [`play.libs.ws.WSBodyWritables`](api/java/play/libs/ws/WSBodyWritables.html), which contains methods for converting input such as `JsonNode` or `XML` in the body of a `WSRequest` into a `ByteString` or `Source<ByteString, ?>`, and  [`play.libs.ws.WSBodyReadables`](api/java/play/libs/ws/WSBodyReadables.html), which contains methods that read the body of a `WSResponse` from a `ByteString` or `Source[ByteString, _]` and return the appropriate type, such as `JsValue` or XML.  The default methods are available to you through the WSRequest and WSResponse, but you can also use custom types with `response.getBody(myReadable())` and `request.post(myWritable(data))`.   This is especially useful if you want to use a custom library, i.e. you would like to stream XML through STaX API.

### Creating a Custom Readable

You can create a custom readable by parsing the response:

@[ws-custom-body-readable](code/javaguide/ws/JavaWS.java)

### Creating a Custom BodyWritable

You can create a custom body writable to a request as follows, using an `InMemoryBodyWritable`.  To specify a custom body writable with streaming, use a `SourceBodyWritable`.

@[ws-custom-body-writable](code/javaguide/ws/JavaWS.java)

## Standalone WS

If you want to call WS outside of Play altogether, you can use the standalone version of Play WS, which does not depend on any Play libraries.  You can do this by adding `play-ahc-ws-standalone` to your project:

```scala
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % playWSStandalone
```

Please see https://github.com/playframework/play-ws and the [[2.6 migration guide|WSMigration26]] for more information.

## Accessing AsyncHttpClient

You can get access to the underlying shaded [AsyncHttpClient](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/AsyncHttpClient.html) from a `WSClient`.

@[ws-underlying-client](code/javaguide/ws/JavaWS.java)

## Configuring WS

Use the following properties in `application.conf` to configure the WS client:

* `play.ws.followRedirects`: Configures the client to follow 301 and 302 redirects *(default is **true**)*.
* `play.ws.useProxyProperties`: To use the system http proxy settings(http.proxyHost, http.proxyPort) *(default is **true**)*.
* `play.ws.useragent`: To configure the User-Agent header field.
* `play.ws.compressionEnabled`: Set it to true to use gzip/deflater encoding *(default is **false**)*.

### Timeouts

There are 3 different timeouts in WS. Reaching a timeout causes the WS request to interrupt.

* `play.ws.timeout.connection`: The maximum time to wait when connecting to the remote host *(default is **120 seconds**)*.
* `play.ws.timeout.idle`: The maximum time the request can stay idle (connection is established but waiting for more data) *(default is **120 seconds**)*.
* `play.ws.timeout.request`: The total time you accept a request to take (it will be interrupted even if the remote host is still sending data) *(default is **120 seconds**)*.

The request timeout can be overridden for a specific connection with `setTimeout()` (see "Making a Request" section).

### Configuring WS with SSL

To configure WS for use with HTTP over SSL/TLS (HTTPS), please see [[Configuring WS SSL|WsSSL]].

### Configuring WS with Caching

To configure WS for use with HTTP caching, please see [[Configuring WS Cache|WsCache]].

### Configuring AsyncClientConfig

The following advanced settings can be configured on the underlying AsyncHttpClientConfig.

Please refer to the [AsyncHttpClientConfig Documentation](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/DefaultAsyncHttpClientConfig.Builder.html) for more information.

* `play.ws.ahc.keepAlive`
* `play.ws.ahc.maxConnectionsPerHost`
* `play.ws.ahc.maxConnectionsTotal`
* `play.ws.ahc.maxConnectionLifetime`
* `play.ws.ahc.idleConnectionInPoolTimeout`
* `play.ws.ahc.maxNumberOfRedirects`
* `play.ws.ahc.maxRequestRetry`
* `play.ws.ahc.disableUrlEncoding`
