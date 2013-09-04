# The Play WS API

Sometimes we would like to call other HTTP services from within a Play application. Play supports this via its [WS library](api/scala/index.html#play.api.libs.ws.package), which provides a way to make asynchronous HTTP calls.

## Imports

To use WS, import the following:

```scala
import play.api.libs.ws._
import scala.concurrent.Future
```

## Making an HTTP call

To send an HTTP request you start with `WS.url()` to specify the URL.  This returns an object called [WSRequestHolder](api/scala/index.html#play.api.libs.ws.WS$$WSRequestHolder) that you can use to specify various HTTP options, such as setting headers. You end by calling a final method corresponding to the HTTP method you want to use.

For example:

```scala
WS.url("http://mysite.com").get()
```

Or:

```scala
WS.url("http://localhost:9001/post").post("content")
```

### Calling with authentication

If you need to use HTTP authentication, you can specify it in the builder, using a username, password, and an [AuthScheme](http://sonatype.github.io/async-http-client/apidocs/reference/com/ning/http/client/Realm.AuthScheme.html).  Options for the AuthScheme are `BASIC`, `DIGEST`, `KERBEROS`, `NONE`, `NTLM`, and `SPNEGO`.

```scala
import com.ning.http.client.Realm.AuthScheme
WS.url("http://example.com/protectedfile").withAuth(user, password, AuthScheme.BASIC).get()
```

### Calling with redirects

If an HTTP call results in a 302 or a 301 redirect, you can automatically follow the redirect without having to make another call.

```scala
WS.url("http://example.com/returns302").withFollowRedirects(true).get()
```

### Calling with query parameters

Parameters can be specified as a series of key/value tuples.

```scala
WS.url("http://localhost:9000").withQueryString("paramKey" -> "paramValue").get()
```

### Calling with additional headers

Headers can be specified as a series of key/value tuples.

```scala
WS.url("http://localhost:9000").withHeaders("headerKey" -> "headerValue").get()
```

### Calling with virtual host

A virtual host can be specified as a string.

```scala
WS.url("http://localhost:9000").withVirtualHost("192.168.1.1").get()
```

### Calling with time out

If you need to give a server more time to process, you can use `withTimeout` to set a value in milliseconds.  You may want to use this for extremely large files.

```scala
WS.url(dataUrl).withTimeout(1000).get()
```

### Using POST with url-form-encoded data

To post url-form-encoded data a `Map[String, Seq[String]]` needs to be passed into `post`.

```scala
WS.url(url).post(Map("key" -> Seq("value")))
```

### Using POST with JSON data

To post JSON data, a `play.api.libs.json.JsObject` needs to be passed into `post`.

```scala
import play.api.libs.json.Json

val data = Json.obj(
  "key1" -> "value1",
  "key2" -> "value2"
)
WS.url(url).post(data)
```

## Retrieving the HTTP response

Any calls made by `WS` should return a `Future[Response]` which we can later handle with Play’s asynchronous mechanisms.

### Processing a response as JSON or XML

???

### Processing a response to a controller

You can compose several promises and end with a `Future[Result]` that can be handled directly by the Play server, using the `Async` method

```scala
def feedTitle(feedUrl: String) = Action {
  Async {
    WS.url(feedUrl).get().map { response =>
      Ok("Feed title: " + (response.json \ "title").as[String])
    }
  }  
}
```

### Processing large responses

Calling `get` or `post` by default will cause the body of the request to be loaded into memory before the response is made available.  When you are downloading with large, multi-gigabyte files, this may result in unwelcome garbage collection or even out of memory errors.  `WS` lets you deal with the download as a stream using [[iteratees|Iteratees]].

@[scalaws-fileupload](code/ScalaWSSpec.scala)

POST and PUT use a slightly different API: instead of `post`, you call `postAndRetrieveStream`:

```scala
WS.url("http://example.com/largedata").postAndRetrieveStream(body) { headers =>
  Iteratee.foreach { bytes => logger.info("Received bytes: " + bytes.length) }
}
```

## Common Patterns and Use Cases

### Chaining WS calls

Using for comprehensions is a good way to chain conversations of WS calls.

```scala
  for {
    jsonPayload <- WS.url("http://example.com/first").get()
    reply = constructReply(jsonPayload)
    response <- WS.url("http://example.com/post").post(reply)
  } yield response
```

### Using Recover

Recover is technically part of the Future, but is widely used to return values.

```
  WS.url("http://example.com").get().recover {
    WS.url("http://backup.example.com").get()
  }
```

??? How to use with for comprehensions?

## Advanced Usage

You can also get access to the underlying [async client](http://sonatype.github.io/async-http-client/apidocs/reference/com/ning/http/client/AsyncHttpClient.html).

```scala
import com.ning.http.client.AsyncHttpClient

val client:AsyncHttpClient = WS.client
```

## Limitations

* `WS` does not support multi part form upload directly.  You can use the underlying client.
* `WS` does not support mutual TLS.
* `WS` does not support faking a user agent.

## Configuring WS 

Use the following properties to configure the WS client

* `ws.timeout` sets both the connection and request timeout in milliseconds
* `ws.followRedirects` configures the client to follow 301 and 302 redirects
* `ws.useProxyProperties`to use the system http proxy settings(http.proxyHost, http.proxyPort) 
* `ws.useragent` to configure the User-Agent header field
* `ws.acceptAnyCertificate` set it to false to use the default SSLContext

 

> **Next:** [[OpenID Support in Play | ScalaOpenID]]
