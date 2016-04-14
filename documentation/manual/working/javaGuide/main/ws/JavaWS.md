<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# The Play WS API

Sometimes we would like to call other HTTP services from within a Play application. Play supports this via its [WS library](api/java/play/libs/ws/package-summary.html), which provides a way to make asynchronous HTTP calls.

There are two important parts to using the WS API: making a request, and processing the response. We'll discuss how to make both GET and POST HTTP requests first, and then show how to process the response from the WS library. Finally, we'll discuss some common use cases.

## Making a Request

To use WS, first add `javaWs` to your `build.sbt` file:

@[javaws-sbt-dependencies](code/javaws.sbt)

Now any controller or component that wants to use WS will have to add the following imports and then declare a dependency on the [`WSClient`](api/java/play/libs/ws/WSClient.html) type to use dependency injection:

@[ws-controller](code/javaguide/ws/Application.java)

To build an HTTP request, you start with `ws.url()` to specify the URL.

@[ws-holder](code/javaguide/ws/JavaWS.java)

This returns a [`WSRequest`](api/java/play/libs/ws/WSRequest.html) that you can use to specify various HTTP options, such as setting headers. You can chain calls together to construct complex requests.

@[ws-complex-holder](code/javaguide/ws/JavaWS.java)

You end by calling a method corresponding to the HTTP method you want to use.  This ends the chain, and uses all the options defined on the built request in the [`WSRequest`](api/java/play/libs/ws/WSRequest.html).

@[ws-get](code/javaguide/ws/JavaWS.java)

This returns a [`CompletionStage<WSResponse>`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) where the [`WSResponse`](api/java/play/libs/ws/WSResponse.html) contains the data returned from the server.

### Request with authentication

If you need to use HTTP authentication, you can specify it in the builder, using a username, password, and an [`WSAuthScheme`](api/java/play/libs/ws/WSAuthScheme.html).  Options for the `WSAuthScheme` are `BASIC`, `DIGEST`, `KERBEROS`, `NTLM`, and `SPNEGO`.

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

### Request with timeout

If you wish to specify a request timeout, you can use `setRequestTimeout` to set a value in milliseconds. A value of `-1` can be used to set an infinite timeout.

@[ws-timeout](code/javaguide/ws/JavaWS.java)

### Submitting form data

To post url-form-encoded data you can set the proper header and formatted data.

@[ws-post-form-data](code/javaguide/ws/JavaWS.java)

### Submitting JSON data

The easiest way to post JSON data is to use the [[JSON library|JavaJsonActions]].

@[json-imports](code/javaguide/ws/JavaWS.java)

@[ws-post-json](code/javaguide/ws/JavaWS.java)

### Submitting multipart/form data

The easiest way to post multipart/form data is to use a `Source<Http.MultipartFormData.Part<Source<ByteString>, ?>, ?>`

@[multipart-imports](code/javaguide/ws/JavaWS.java)

@[ws-post-multipart](code/javaguide/ws/JavaWS.java)

To Upload a File you need to pass a `Http.MultipartFormData.FilePart<Source<ByteString>, ?>` to the `Source`:

@[ws-post-multipart2](code/javaguide/ws/JavaWS.java)

### Streaming data

It's also possible to stream data.

Here is an example showing how you could stream a large image to a different endpoint for further processing:

@[ws-stream-request](code/javaguide/ws/JavaWS.java)

The `largeImage` in the code snippet above is an Akka Streams `Source<ByteString, ?>`.

### Request Filters

You can do additional processing on a WSRequest by adding a request filter.  A request filter is added by extending the [`play.libs.ws.WSRequestFilter`](api/java/play/libs/ws/WSRequestFilter.html) trait, and then adding it to the request with `request.withRequestFilter(filter)`.  

@[ws-request-filter](code/javaguide/ws/JavaWS.java)

## Processing the Response

Working with the [`WSResponse`](api/java/play/libs/ws/WSResponse.html) is done by applying transformations such as `thenApply` and `thenCompose` to the `CompletionStage`.

### Processing a response as JSON

You can process the response as a `JsonNode` by calling `response.asJson()`.

@[ws-response-json](code/javaguide/ws/JavaWS.java)


### Processing a response as XML

Similarly, you can process the response as XML by calling `response.asXml()`.

@[ws-response-xml](code/javaguide/ws/JavaWS.java)

### Processing large responses

Calling `get()`, `post()` or `execute()` will cause the body of the response to be loaded into memory before the response is made available.  When you are downloading a large, multi-gigabyte file, this may result in unwelcomed garbage collection or even out of memory errors.

`WS` lets you consume the response's body incrementally by using an Akka Streams `Sink`.  The `stream()` method on `WSRequest` returns a `CompletionStage<StreamedResponse>`. A `StreamedResponse` is a simple container holding together the response's headers and body.

Any controller or component that wants to leverage the WS streaming functionality will have to add the following imports and dependencies:

@[ws-streams-controller](code/javaguide/ws/MyController.java)

Here is a trivial example that uses a folding `Sink` to count the number of bytes returned by the response:

@[stream-count-bytes](code/javaguide/ws/JavaWS.java)

Alternatively, you could also stream the body out to another location. For example, a file:

@[stream-to-file](code/javaguide/ws/JavaWS.java)

Another common destination for response bodies is to stream them back from a controller's `Action`:

@[stream-to-result](code/javaguide/ws/JavaWS.java)

As you may have noticed, before calling `stream()` we need to set the HTTP method to use by calling `setMethod` on the request. Here follows another example that uses `PUT` instead of `GET`:

@[stream-put](code/javaguide/ws/JavaWS.java)

Of course, you can use any other valid HTTP verb.

## Common Patterns and Use Cases

### Chaining WS calls

You can chain WS calls by using `flatMap`.

@[ws-composition](code/javaguide/ws/JavaWS.java)

### Exception recovery
If you want to recover from an exception in the call, you can use `recover` or `recoverWith` to substitute a response.

@[ws-recover](code/javaguide/ws/JavaWS.java)

### Using in a controller

You can map a `CompletionStage<WSResponse>` to a `CompletionStage<Result>` that can be handled directly by the Play server, using the asynchronous action pattern defined in [[Handling Asynchronous Results|JavaAsync]].

@[ws-action](code/javaguide/ws/JavaWS.java)

## Using WSClient

We recommend that you get your `WSClient` instances using [[dependency injection|JavaDependencyInjection]] as described above. `WSClient` instances created through dependency injection are simpler to use because they are automatically created when the application starts and cleaned up when the application stops.

However, if you choose, you can instantiate a `WSClient` directly from code and use this for making requests or for configuring underlying `AsyncHttpClient` options.

> **Note:** If you create a `WSClient` manually then you **must** call `client.close()` to clean it up when you've finished with it. Each client creates its own thread pool. If you fail to close the client or if you create too many clients then you will run out of threads or file handles -— you'll get errors like "Unable to create new native thread" or "too many open files" as the underlying resources are consumed.

Here is an example of how to create a `WSClient` instance by yourself:

@[ws-custom-client-imports](code/javaguide/ws/JavaWS.java)

@[ws-custom-client](code/javaguide/ws/JavaWS.java)

@[ws-client](code/javaguide/ws/JavaWS.java)

Once you are done with your custom client work, you **must** close the client:

@[ws-close-client](code/javaguide/ws/JavaWS.java)

Ideally, you should only close a client after you know all requests have been completed.  You should not use [`try-with-resources`](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) to automatically close a WSClient instance, because WSClient logic is asynchronous and `try-with-resources` only supports synchronous code in its body.

## Accessing AsyncHttpClient

You can get access to the underlying [AsyncHttpClient](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/AsyncHttpClient.html) from a `WSClient`.

@[ws-underlying-client](code/javaguide/ws/JavaWS.java)

This is important in a couple of cases. The WS library has a couple of limitations that require access to the underlying client:

* `WS` does not support streaming body upload.  In this case, you should use the [`FeedableBodyGenerator`](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/request/body/generator/FeedableBodyGenerator.html) provided by AsyncHttpClient.

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

## Configuring WS with SSL

To configure WS for use with HTTP over SSL/TLS (HTTPS), please see [[Configuring WS SSL|WsSSL]].

### Configuring AsyncClientConfig

The following advanced settings can be configured on the underlying AsyncHttpClientConfig.

Please refer to the [AsyncHttpClientConfig Documentation](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/DefaultAsyncHttpClientConfig.Builder.html) for more information.

> **Note:** `allowPoolingConnection` and `allowSslConnectionPool` are combined in AsyncHttpClient 2.0 into a single `keepAlive` variable.  As such, `play.ws.ning.allowPoolingConnection` and `play.ws.ning.allowSslConnectionPool` are not valid and will throw an exception if configured.

* `play.ws.ahc.keepAlive`
* `play.ws.ahc.maxConnectionsPerHost`
* `play.ws.ahc.maxConnectionsTotal`
* `play.ws.ahc.maxConnectionLifeTime`
* `play.ws.ahc.idleConnectionInPoolTimeout`
* `play.ws.ahc.maxNumberOfRedirects`
* `play.ws.ahc.maxRequestRetry`
* `play.ws.ahc.disableUrlEncoding`
