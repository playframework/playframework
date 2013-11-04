# The Play WS API

Sometimes we would like to call other HTTP services from within a Play application. Play supports this via its [WS library](api/scala/index.html#play.api.libs.ws.package), which provides a way to make asynchronous HTTP calls.

There are two important parts to using the WS API: making a request, and processing the response.  We'll discuss how to make both GET and POST HTTP requests first, and then show how to process the response from WS.  Finally, we'll discuss some common use cases.

## Making a Request

To use WS, first import the following:

```scala
import play.api.libs.ws._
import scala.concurrent.Future
```

To build an HTTP request, you start with `WS.url()` to specify the URL.

```scala
val holder : WSRequestHolder = WS.url(url)
```

This returns a [WSRequestHolder](api/scala/index.html#play.api.libs.ws.WS$$WSRequestHolder) that you can use to specify various HTTP options, such as setting headers.  You can chain calls together to construct complex requests.

```scala
val complexHolder : WSRequestHolder = holder.withHeaders(...)
                                            .withTimeout(...)
                                            .withQueryString(...)
```

You end by calling a method corresponding to the HTTP method you want to use.  This ends the chain, and uses all the options defined on the built request in the `WSRequestHolder`.

```scala
val futureResponse : Future[Response] = complexHolder.get()
```

This returns a `Future[Response]` where the [Response](api/scala/index.html#play.api.libs.ws.Response) contains the data returned from the server.

### Request with authentication

If you need to use HTTP authentication, you can specify it in the builder, using a username, password, and an [AuthScheme](http://sonatype.github.io/async-http-client/apidocs/reference/com/ning/http/client/Realm.AuthScheme.html).  Options for the AuthScheme are `BASIC`, `DIGEST`, `KERBEROS`, `NONE`, `NTLM`, and `SPNEGO`.

```scala
import com.ning.http.client.Realm.AuthScheme

WS.url(url).withAuth(user, password, AuthScheme.BASIC).get()
```

### Request with follow redirects

If an HTTP call results in a 302 or a 301 redirect, you can automatically follow the redirect without having to make another call.

```scala
WS.url(url).withFollowRedirects(true).get()
```

### Request with query parameters

Parameters can be specified as a series of key/value tuples.

```scala
WS.url(url).withQueryString("paramKey" -> "paramValue").get()
```

### Request with additional headers

Headers can be specified as a series of key/value tuples.

```scala
WS.url(url).withHeaders("headerKey" -> "headerValue").get()
```

If you are sending plain text in a particular format, you may want to define the content type explicitly.

```scala
WS.url(url).withHeaders("Content-Type" -> "text-xml").post(xmlString)
```

### Request with virtual host

A virtual host can be specified as a string.

```scala
WS.url(url).withVirtualHost("192.168.1.1").get()
```

### Request with time out

If you need to give a server more time to process, you can use `withTimeout` to set a value in milliseconds.  You may want to use this for extremely large files.

```scala
WS.url(url).withTimeout(1000).get()
```

### Submitting form data

To post url-form-encoded data a `Map[String, Seq[String]]` needs to be passed into `post`.

```scala
WS.url(url).post(Map("key" -> Seq("value")))
```

### Submitting JSON data

The easiest way to post JSON data is to use the [[JSON|ScalaJson]] library.

@[scalaws-post-json](code/ScalaWSSpec.scala)

### Submitting XML data

The easiest way to post XML data is to use XML literals.  XML literals are convenient, but not very fast.  For efficiency, consider using an XML view template, or a JAXB library.

@[scalaws-post-xml](code/ScalaWSSpec.scala)

## Processing the Response

Working with the [Response](api/scala/index.html#play.api.libs.ws.Response) is easily done by mapping inside the [Future](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future).

The examples given below have some common dependencies that will be shown once here for brevity.

An execution context, required for Future.map:

@[scalaws-context](code/ScalaWSSpec.scala)

and a case class that will be used for serialization / deserialization:

@[scalaws-person](code/ScalaWSSpec.scala)

### Processing a response as JSON

You can process the response as a [JSON object](api/scala/index.html#play.api.libs.json.JsValue) by calling `response.json`.

@[scalaws-process-json](code/ScalaWSSpec.scala)

The JSON library has a [[useful feature|ScalaJsonCombinators]] that will map an implicit [`Reads[T]`](api/scala/index.html#play.api.libs.json.Reads) directly to a class:

@[scalaws-process-json-with-implicit](code/ScalaWSSpec.scala)

### Processing a response as XML

You can process the response as an [XML literal](http://www.scala-lang.org/api/current/index.html#scala.xml.NodeSeq) by calling `response.xml`.

@[scalaws-process-xml](code/ScalaWSSpec.scala)

### Processing large responses

Calling `get()` or `post()` will cause the body of the request to be loaded into memory before the response is made available.  When you are downloading with large, multi-gigabyte files, this may result in unwelcome garbage collection or even out of memory errors.

`WS` lets you use the response incrementally by using an [[iteratee|Iteratees]].

@[scalaws-fileupload](code/ScalaWSSpec.scala)

This is an iteratee that will receive a portion of the file as an array of bytes, write those bytes to an OutputStream, and close the stream when it receives the `EOF` signal.  Until it receives an `EOF` signal, the iteratee will keep running.

`WS` doesn't send `EOF` to the iteratee when it's finished -- instead, it redeems the returned future.
In fact, `WS` has no right to feed `EOF`, since it doesn't control the input.  You may want to feed the result of multiple WS calls into that iteratee (maybe you're building a tar file on the fly), and if `WS` feeds `EOF`, the stream will close unexpectedly.  Sending `EOF` to the stream is the caller's responsibility.

We do this by calling [Iteratee.run](http://www.playframework.com/documentation/2.1.x/api/scala/index.html#play.api.libs.iteratee.Iteratee) which will push an `EOF` into the iteratee when the future is redeemed.

`POST` and `PUT` calls use a slightly different API than `GET` calls: instead of `post()`, you call `postAndRetrieveStream(body)` which has the same effect.

```scala
WS.url(url).postAndRetrieveStream(body) { headers =>
  Iteratee.foreach { bytes => logger.info("Received bytes: " + bytes.length) }
}
```

## Common Patterns and Use Cases

### Chaining WS calls

Using for comprehensions is a good way to chain WS calls in a trusted environment.  You should use for comprehensions together with [Future.recover](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) to handle possible failure.

@[scalaws-forcomprehension](code/ScalaWSSpec.scala)

### Using in a controller

You can compose several promises and end with a `Future[Result]` that can be handled directly by the Play server, using the `Action.async` builder defined in [[Handling Asynchronous Results|ScalaAsync]].

```scala
def feedTitle(feedUrl: String) = Action.async {
  WS.url(feedUrl).get().map { response =>
    Ok("Feed title: " + (response.json \ "title").as[String])
  }
}
```

## Advanced Usage

You can also get access to the underlying [async client](http://sonatype.github.io/async-http-client/apidocs/reference/com/ning/http/client/AsyncHttpClient.html).

```scala
import com.ning.http.client.AsyncHttpClient

val client:AsyncHttpClient = WS.client
```

This is important in a couple of cases.  WS has a couple of limitations that require access to the client:

* `WS` does not support multi part form upload directly.  You can use the underlying client with [RequestBuilder.addBodyPart](http://asynchttpclient.github.io/async-http-client/apidocs/com/ning/http/client/RequestBuilder.html).
* `WS` does not support client certificates (aka mutual TLS / MTLS / client authentication).  You should set the `SSLContext` directly in an instance of [AsyncHttpClientConfig](http://asynchttpclient.github.io/async-http-client/apidocs/com/ning/http/client/AsyncHttpClientConfig.html) and set up the appropriate KeyStore and TrustStore.

## Configuring WS 

Use the following properties to configure the WS client

* `ws.timeout` sets both the connection and request timeout in milliseconds
* `ws.followRedirects` configures the client to follow 301 and 302 redirects
* `ws.useProxyProperties`to use the system http proxy settings(http.proxyHost, http.proxyPort) 
* `ws.useragent` to configure the User-Agent header field
* `ws.acceptAnyCertificate` set it to false to use the default SSLContext

 
### Timeouts

There are 3 different timeouts in WS. Reaching a timeout causes the WS request to interrupt.

* **Connection Timeout**: The maximum time to wait when connecting to the remote host *(default is **120 seconds**)*.
* **Connection Idle Timeout**: The maximum time the request can stay idle (connexion is established but waiting for more data) *(default is **120 seconds**)*.
* **Request Timeout**: The total time you accept a request to take (it will be interrupted, whatever if the remote host is still sending data) *(default is **none**, to allow stream consuming)*.

You can define each timeout in `application.conf` with respectively: `ws.timeout.connection`, `ws.timeout.idle`, `ws.timeout.request`.

Alternatively, `ws.timeout` can be defined to target both *Connection Timeout* and *Connection Idle Timeout*.

The request timeout can be specified for a given connection with `withRequestTimeout`.

Example:

```scala
WS.url("http://playframework.org/").withRequestTimeout(10000 /* in milliseconds */)
```

> **Next:** [[OpenID Support in Play|ScalaOpenID]]
