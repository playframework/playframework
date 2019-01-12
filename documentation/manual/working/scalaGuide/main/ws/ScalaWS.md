<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Calling REST APIs with Play WS

Sometimes we would like to call other HTTP services from within a Play application. Play supports this via its [WS library](api/scala/play/api/libs/ws/index.html), which provides a way to make asynchronous HTTP calls through a WSClient instance.

There are two important parts to using the WSClient: making a request, and processing the response.  We'll discuss how to make both GET and POST HTTP requests first, and then show how to process the response from WSClient.  Finally, we'll discuss some common use cases.

> **Note**: In Play 2.6, Play WS has been split into two, with an underlying standalone client that does not depend on Play, and a wrapper on top that uses Play specific classes.  In addition, shaded versions of AsyncHttpClient and Netty are now used in Play WS to minimize library conflicts, primarily so that Play's HTTP engine can use a different version of Netty.  Please see the [[2.6 migration guide|WSMigration26]] for more information.

## Adding WS to project

To use WSClient, first add `ws` to your `build.sbt` file:

```scala
libraryDependencies += ws
```

## Enabling HTTP Caching in Play WS

Play WS supports [HTTP caching](https://tools.ietf.org/html/rfc7234), but requires a JSR-107 cache implementation to enable this feature.  You can add `ehcache`:

```scala
libraryDependencies += ehcache
```

Or you can use another JSR-107 compatible cache such as [Caffeine](https://github.com/ben-manes/caffeine/wiki/JCache).

Once you have the library dependencies, then enable the HTTP cache as shown on [[WS Cache Configuration|WsCache]] page.

Using an HTTP cache means savings on repeated requests to backend REST services, and is especially useful when combined with resiliency features such as [`stale-on-error` and `stale-while-revalidate`](https://tools.ietf.org/html/rfc5861).

## Making a Request

Now any component that wants to use WS will have to declare a dependency on the `WSClient`:

@[dependency](code/ScalaWSSpec.scala)

We've called the `WSClient` instance `ws`, all the following examples will assume this name.

To build an HTTP request, you start with `ws.url()` to specify the URL.

@[simple-holder](code/ScalaWSSpec.scala)

This returns a [WSRequest](api/scala/play/api/libs/ws/WSRequest.html) that you can use to specify various HTTP options, such as setting headers.  You can chain calls together to construct complex requests.

@[complex-holder](code/ScalaWSSpec.scala)

You end by calling a method corresponding to the HTTP method you want to use.  This ends the chain, and uses all the options defined on the built request in the `WSRequest`.

@[holder-get](code/ScalaWSSpec.scala)

This returns a `Future[WSResponse]` where the [Response](api/scala/play/api/libs/ws/WSResponse.html) contains the data returned from the server.

> If you are doing any blocking work, including any kind of DNS work such as calling [`java.util.URL.equals()`](https://docs.oracle.com/javase/8/docs/api/java/net/URL.html#equals-java.lang.Object-), then you should use a custom execution context as described in [[ThreadPools]], preferably through a [`CustomExecutionContext`](api/scala/play/api/libs/concurrent/CustomExecutionContext.html).  You should size the pool to leave a safety margin large enough to account for failures. 

> If you are calling out to an [unreliable network](https://queue.acm.org/detail.cfm?id=2655736), consider using [`Futures.timeout`](api/scala/play/api/libs/concurrent/Futures.html) and a [circuit breaker](https://martinfowler.com/bliki/CircuitBreaker.html) like [Failsafe](https://github.com/jhalterman/failsafe#circuit-breakers).

### Request with authentication

If you need to use HTTP authentication, you can specify it in the builder, using a username, password, and an `AuthScheme`.  Valid case objects for the AuthScheme are `BASIC`, `DIGEST`, `KERBEROS`, `NTLM`, and `SPNEGO`.

@[auth-request](code/ScalaWSSpec.scala)

### Request with follow redirects

If an HTTP call results in a 302 or a 301 redirect, you can automatically follow the redirect without having to make another call.

@[redirects](code/ScalaWSSpec.scala)

### Request with query parameters

Parameters can be specified as a series of key/value tuples.  Use [`addQueryStringParameters`](api/scala/play/api/libs/ws/WSRequest.html#addQueryStringParameters\(parameters:\(String,String\)*\):StandaloneWSRequest.this.Self) to add parameters, and [`withQueryStringParameters`](api/scala/play/api/libs/ws/WSRequest.html#withQueryStringParameters\(parameters:\(String,String\)*\):WSRequest.this.Self) to overwrite all query string parameters.

@[query-string](code/ScalaWSSpec.scala)

### Request with additional headers

Headers can be specified as a series of key/value tuples.  Use `addHttpHeaders` to append additional headers, and `withHttpHeaders` to overwrite all headers.

@[headers](code/ScalaWSSpec.scala)

If you are sending plain text in a particular format, you may want to define the content type explicitly.

@[content-type](code/ScalaWSSpec.scala)

### Request with cookies

Cookies can be added to the request by using `DefaultWSCookie` or by passing through [`play.api.mvc.Cookie`](api/scala/play/api/mvc/Cookie.html).  Use [`addCookies`](api/scala/play/api/libs/ws/WSRequest.html#addCookies\(cookies:play.api.libs.ws.WSCookie*\):StandaloneWSRequest.this.Self) to append cookies, and [`withCookies`](api/scala/play/api/libs/ws/WSRequest.html#withCookies\(cookie:play.api.libs.ws.WSCookie*\):WSRequest.this.Self) to overwrite all cookies.

@[cookie](code/ScalaWSSpec.scala)

### Request with virtual host

A virtual host can be specified as a string.

@[virtual-host](code/ScalaWSSpec.scala)

### Request with timeout

If you wish to specify a request timeout, you can use [`withRequestTimeout`](api/scala/play/api/libs/ws/WSRequest.html#withRequestTimeout\(timeout:scala.concurrent.duration.Duration\):WSRequest.this.Self) to set a value. An infinite timeout can be set by passing `Duration.Inf`.

@[request-timeout](code/ScalaWSSpec.scala)

### Submitting form data

To post url-form-encoded data a `Map[String, Seq[String]]` needs to be passed into `post`.
If the body is empty, you must pass play.api.libs.ws.EmptyBody into the post method.

@[url-encoded](code/ScalaWSSpec.scala)

### Submitting multipart/form data

To post multipart-form-encoded data a `Source[play.api.mvc.MultipartFormData.Part[Source[ByteString, Any]], Any]` needs to be passed into `post`.

@[multipart-encoded](code/ScalaWSSpec.scala)

To upload a file you need to pass a `play.api.mvc.MultipartFormData.FilePart[Source[ByteString, Any]]` to the `Source`:

@[multipart-encoded2](code/ScalaWSSpec.scala)

### Submitting JSON data

The easiest way to post JSON data is to use the [[JSON|ScalaJson]] library.

@[scalaws-post-json](code/ScalaWSSpec.scala)

### Submitting XML data

The easiest way to post XML data is to use XML literals.  XML literals are convenient, but not very fast.  For efficiency, consider using an XML view template, or a JAXB library.

@[scalaws-post-xml](code/ScalaWSSpec.scala)

### Submitting Streaming data

It's also possible to stream data in the request body using [Akka Streams](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html?language=scala).

For example, imagine you have executed a database query that is returning a large image, and you would like to forward that data to a different endpoint for further processing. Ideally, if you can send the data as you receive it from the database, you will reduce latency and also avoid problems resulting from loading in memory a large set of data. If your database access library supports [Reactive Streams](http://www.reactive-streams.org/) (for instance, [Slick](http://slick.typesafe.com/) does), here is an example showing how you could implement the described behavior:

@[scalaws-stream-request](code/ScalaWSSpec.scala)

The `largeImageFromDB` in the code snippet above is a `Source[ByteString, _]`.

### Request Filters

You can do additional processing on a WSRequest by adding a request filter.  A request filter is added by extending the `play.api.libs.ws.WSRequestFilter` trait, and then adding it to the request with `request.withRequestFilter(filter)`.

A sample request filter that logs the request in cURL format to SLF4J has been added in `play.api.libs.ws.ahc.AhcCurlRequestLogger`.

@[curl-logger-filter](code/ScalaWSSpec.scala)

will output:

```
curl \
  --verbose \
  --request PUT \
 --header 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
 --data 'key=value' \
 http://localhost:19001/
```

## Processing the Response

Working with the [Response](api/scala/play/api/libs/ws/WSResponse.html) is easily done by mapping inside the [Future](https://www.scala-lang.org/api/current/index.html#scala.concurrent.Future).

The examples given below have some common dependencies that will be shown once here for brevity.

Whenever an operation is done on a `Future`, an implicit execution context must be available - this declares which thread pool the callback to the future should run in.  You can inject the default Play execution context in your DI-ed class by declaring an additional dependency to `ExecutionContext` in the class' constructor:

@[scalaws-context-injected](code/ScalaWSSpec.scala)

The examples also use the following case class for serialization/deserialization:

@[scalaws-person](code/ScalaWSSpec.scala)

The WSResponse extends [`play.api.libs.ws.WSBodyReadables`](api/scala/play/api/libs/ws/WSBodyReadables.html) trait, which contains type classes for Play JSON and Scala XML conversion.  You can also create your own custom type classes if you would like to convert the response to your own types, or use a different JSON or XML encoding. 

### Processing a response as JSON

You can process the response as a [JSON object](https://static.javadoc.io/com.typesafe.play/play-json_2.12/2.6.9/play/api/libs/json/JsValue.html) by calling `response.json`.

@[scalaws-process-json](code/ScalaWSSpec.scala)

The JSON library has a [[useful feature|ScalaJsonCombinators]] that will map an implicit [`Reads[T]`](https://static.javadoc.io/com.typesafe.play/play-json_2.12/2.6.9/play/api/libs/json/Reads.html) directly to a class:

@[scalaws-process-json-with-implicit](code/ScalaWSSpec.scala)

### Processing a response as XML

You can process the response as an [XML literal](https://www.scala-lang.org/api/current/index.html#scala.xml.NodeSeq) by calling `response.xml`.

@[scalaws-process-xml](code/ScalaWSSpec.scala)

### Processing large responses

Calling `get()`, `post()` or `execute()` will cause the body of the response to be loaded into memory before the response is made available.  When you are downloading a large, multi-gigabyte file, this may result in unwelcome garbage collection or even out of memory errors.

`WS` lets you consume the response's body incrementally by using an [Akka Streams](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html?language=scala) `Sink`.  The [`stream()`](api/scala/play/api/libs/ws/WSRequest.html#stream\(\):scala.concurrent.Future[StandaloneWSRequest.this.Response]) method on `WSRequest` returns a streaming `WSResponse` which contains a [`bodyAsSource`](api/scala/play/api/libs/ws/WSResponse.html#bodyAsSource:akka.stream.scaladsl.Source[akka.util.ByteString,_]) method that returns a `Source[ByteString, _]`  

> **Note**: In 2.5.x, a `StreamedResponse` was returned in response to a `request.stream()` call.  In 2.6.x, a standard [`WSResponse`](api/scala/play/api/libs/ws/WSResponse.html) is returned, and the `getBodyAsSource()` method should be used to return the Source.

Here is a trivial example that uses a folding `Sink` to count the number of bytes returned by the response:

@[stream-count-bytes](code/ScalaWSSpec.scala)

Alternatively, you could also stream the body out to another location. For example, a file:

@[stream-to-file](code/ScalaWSSpec.scala)

Another common destination for response bodies is to stream them back from a controller's `Action`:

@[stream-to-result](code/ScalaWSSpec.scala)

As you may have noticed, before calling [`stream()`](api/scala/play/api/libs/ws/WSRequest.html#stream\(\):scala.concurrent.Future[StandaloneWSRequest.this.Response]) we need to set the HTTP method to use by calling `withMethod` on the request. Here follows another example that uses `PUT` instead of `GET`:

@[stream-put](code/ScalaWSSpec.scala)

Of course, you can use any other valid HTTP verb.

## Common Patterns and Use Cases

### Chaining WSClient calls

Using for comprehensions is a good way to chain WSClient calls in a trusted environment.  You should use for comprehensions together with [Future.recover](https://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) to handle possible failure.

@[scalaws-forcomprehension](code/ScalaWSSpec.scala)

### Using in a controller

When making a request from a controller, you can map the response to a `Future[Result]`. This can be used in combination with Play's `Action.async` action builder, as described in [[Handling Asynchronous Results|ScalaAsync]].

@[async-result](code/ScalaWSSpec.scala)

### Using WSClient with Future Timeout

If a chain of WS calls does not complete in time, it may be useful to wrap the result in a timeout block, which will return a failed Future if the chain does not complete in time -- this is more generic than using `withRequestTimeout`, which only applies to a single request.  The best way to do this is with Play's [[non-blocking timeout feature|ScalaAsync]], using [`play.api.libs.concurrent.Futures`](api/scala/play/api/libs/concurrent/Futures.html):

@[ws-futures-timeout](code/ScalaWSSpec.scala)

## Compile Time Dependency Injection

If you are using compile time dependency injection, you can access a `WSClient` instance by using the trait `AhcWSComponents`.

## Directly creating WSClient

We recommend that you get your `WSClient` instances using dependency injection as described above. `WSClient` instances created through dependency injection are simpler to use because they are automatically created when the application starts and cleaned up when the application stops.

However, if you choose, you can instantiate a `WSClient` directly from code and use this for making requests or for configuring underlying `AsyncHttpClient` options.

> **If you create a WSClient manually then you _must_ call `client.close()` to clean it up when you've finished with it.** Each client creates its own thread pool. If you fail to close the client or if you create too many clients then you will run out of threads or file handles -— you'll get errors like "Unable to create new native thread" or "too many open files" as the underlying resources are consumed.

You need an instance of an `akka.stream.Materializer` to create a `play.api.libs.ws.ahc.AhcWSClient` instance directly.  Usually you'll inject this into the service using dependency injection:

@[simple-ws-custom-client](code/ScalaWSSpec.scala)

Creating a client directly means that you can also change configuration at the AsyncHttpClient and Netty configuration layers as well:

@[ws-custom-client](code/ScalaWSSpec.scala)

You can also use [`play.api.test.WsTestClient.withTestClient`](api/scala/play/api/test/WsTestClient.html) to create an instance of `WSClient` in a functional test.  See [[ScalaTestingWebServiceClients]] for more details.

Or, you can run the `WSClient` completely standalone without involving a running Play application at all:

@[ws-standalone](code/ScalaWSStandalone.scala)

Again, once you are done with your custom client work, you **must** close the client:

@[close-client](code/ScalaWSSpec.scala)

Ideally, you should close a client after you know all requests have been completed.  Be careful of using an automatic resource management pattern to close the client, because WSClient logic is asynchronous and many ARM solutions may be designed for a single threaded synchronous solution.

## Standalone WS

If you want to call WS outside of the context of Play altogether, you can use the standalone version of Play WS, which does not depend on any Play libraries.  You can do this by adding `play-ahc-ws-standalone` to your project:

```scala
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % playWSStandalone
```

Please see https://github.com/playframework/play-ws and the [[2.6 migration guide|WSMigration26]] for more information.

## Custom BodyReadables and BodyWritables

Play WS comes with rich type support for bodies in the form of [`play.api.libs.ws.WSBodyWritables`](api/scala/play/api/libs/ws/WSBodyWritables.html), which contains type classes for converting input such as `JsValue` or `XML` in the body of a `WSRequest` into a `ByteString` or `Source[ByteString, _]`, and  [`play.api.libs.ws.WSBodyReadables`](api/scala/play/api/libs/ws/WSBodyReadables.html), which aggregates type classes that read the body of a `WSResponse` from a `ByteString` or `Source[ByteString, _]` and return the appropriate type, such as `JsValue` or XML.  These type classes are automatically in scope when you import the ws package, but you can also create custom types.  This is especially useful if you want to use a custom library, i.e. you would like to stream XML through STaX API or use another JSON library such as Argonaut or Circe.

### Creating a Custom Readable

You can create a custom readable by accessing the response body:

@[ws-custom-body-readable](code/ScalaWSSpec.scala)

### Creating a Custom BodyWritable

You can create a custom body writable to a request as follows, using an `BodyWritable` and an `InMemoryBody`.  To specify a custom body writable with streaming, use a `SourceBody`.

@[ws-custom-body-writable](code/ScalaWSSpec.scala)

## Accessing AsyncHttpClient

You can get access to the underlying [AsyncHttpClient](http://static.javadoc.io/org.asynchttpclient/async-http-client/2.0.0/org/asynchttpclient/AsyncHttpClient.html) from a `WSClient`.

@[underlying](code/ScalaWSSpec.scala)

## Configuring WSClient

Use the following properties in `application.conf` to configure the WSClient:

* `play.ws.followRedirects`: Configures the client to follow 301 and 302 redirects *(default is **true**)*.
* `play.ws.useProxyProperties`: To use the JVM system's HTTP proxy settings (http.proxyHost, http.proxyPort) *(default is **true**)*.
* `play.ws.useragent`: To configure the User-Agent header field.
* `play.ws.compressionEnabled`: Set it to true to use gzip/deflater encoding *(default is **false**)*.

### Configuring WSClient with SSL

To configure WS for use with HTTP over SSL/TLS (HTTPS), please see [[Configuring WS SSL|WsSSL]].

### Configuring WS with Caching

To configure WS for use with HTTP caching, please see [[Configuring WS Cache|WsCache]].

### Configuring Timeouts

There are 3 different timeouts in WSClient. Reaching a timeout causes the WSClient request to interrupt.

* `play.ws.timeout.connection`: The maximum time to wait when connecting to the remote host *(default is **120 seconds**)*.
* `play.ws.timeout.idle`: The maximum time the request can stay idle (connection is established but waiting for more data) *(default is **120 seconds**)*.
* `play.ws.timeout.request`: The total time you accept a request to take (it will be interrupted even if the remote host is still sending data) *(default is **120 seconds**)*.

The request timeout can be overridden for a specific connection with `withRequestTimeout()` (see "Making a Request" section).

### Configuring AsyncHttpClientConfig

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
