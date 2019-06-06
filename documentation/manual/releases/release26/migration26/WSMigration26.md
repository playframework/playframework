<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Play WS Migration Guide

Play WS now has a standalone version - [https://github.com/playframework/play-ws](https://github.com/playframework/play-ws) - that can be used outside a Play project. If you have a Play sbt project, you can still add WS by adding the following line to your `build.sbt`:
 
```scala
libraryDependencies += ws
```

This includes the `play-ahc-ws` module, which wraps the standalone version with Play Dependency Injection bindings and components, configuration and anything else that is necessary to better integrate it.


And if you want to use the cache support, you need to add `ws`, `ehcache` and [[enable and configure cache|WsCache]]:

```scala
libraryDependencies += ws
libraryDependencies += ehcache
```

If you want to use it in a non Play project, it can be added to an sbt project with:

```scala
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.2"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.2"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-xml" % "1.1.2"
```

## Package changes

Play WS historically consisted of two libraries, `ws` and `playWs`, containing the Scala and Java APIs respectively, each individually creating an AsyncHTTPClient behind the scenes.  There is now only one `play-ahc-ws` library, which contains both Scala and Java `WSClient` instances, and both point to a singleton `AsyncHttpClient` provider.

## Project changes

Play WS now exists as a Play specific wrapper on top of a standalone WS library, which does not depend on Play classes, and which uses package renamed "shaded" versions of AsyncHttpClient, Signpost, and Netty 4.0.

By providing a standalone WS version and using shaded libraries, WS is more flexible and has fewer collisions with other libraries and projects.

The Play WS API extends Standalone WS `post` with `Http.Multipart` and `Multipart` types that are only available in Play, for example:

```scala
def withBody(body: Source[MultipartFormData.Part[Source[ByteString, _]], _]): Self 
```

Signpost OAuth has been changed so that instead of using the Commons HTTPClient OAuthProvider, it now uses the DefaultOAuthProvider, which uses HTTPURLConnection under the hood.

## API changes

### Scala

The `WSAPI` class has been removed.  The `WSClient` interface is the point of entry for the WS API.

`WSRequest` had a `withBody[T](body: T)(implicit writable: play.api.http.Writable[T])` method has been replaced as it was difficult to track the behavior of `Writable`. There is now a custom `BodyWritable[T]` type class that fills the same function, and which has type class instances defined in Standalone WS:

```scala
override def withBody[T: BodyWritable](body: T)
```

The deprecated Scala singleton object `play.api.libs.ws.WS` has been removed.  An instance of `WSClient` should be used instead.  If compile time dependency injection is being used, then the `AhcWSComponents` trait should be mixed in.

For Guice, there is a `WSClient` available in the system:

```scala
class MyService @Inject()(ws: WSClient) {
   def call(): Unit = {     
     ws.url("http://localhost:9000/foo").get()
   }
}
```

If you cannot use an injected WSClient instance, then you can also create your [[own instance of WSClient|ScalaWS#using-wsclient]], but you are then responsible for managing the lifecycle of the client.

If you are running a functional test, you can use the `play.api.test.WsTestClient`, which will start up and shut down a standalone WSClient instance:

```scala
play.api.test.WsTestClient.withClient { ws =>
  ws.url("http://localhost:9000/foo").get()
}
```

The `ning` package has been replaced by the `ahc` package, and the Ning* classes replaced by AHC*.

A normal `WSResponse` instance is returned from `stream()` instead of `StreamedResponse`.  You should call `response.bodyAsSource` to return the streamed result.

There are some naming changes with deprecations to make things more explicit on `play.api.libs.ws.WSRequest`. Extra care should be exercised when migrating these:
- [`WsRequest.withHeaders`](https://playframework.com/documentation/2.6.x/api/scala/index.html#play.api.libs.ws.WSRequest@withHeaders\(headers:\(String,String\)*\):WSRequest.this.Self) is now [`WsRequest.addHttpHeaders`](https://playframework.com/documentation/2.6.x/api/scala/index.html#play.api.libs.ws.WSRequest@addHttpHeaders\(hdrs:\(String,String\)*\):StandaloneWSRequest.this.Self) (same behaviour) or [`WsRequest.withHttpHeaders`](https://playframework.com/documentation/2.6.x/api/scala/index.html#play.api.libs.ws.WSRequest@withHttpHeaders\(headers:\(String,String\)*\):WSRequest.this.Self) (throws away existing headers)
- [`WsRequest.withQueryString`](https://playframework.com/documentation/2.6.x/api/scala/index.html#play.api.libs.ws.WSRequest@withQueryString\(parameters:\(String,String\)*\):WSRequest.this.Self) is now [`WsRequest.addQueryStringParameters`](https://playframework.com/documentation/2.6.x/api/scala/index.html#play.api.libs.ws.WSRequest@addQueryStringParameters\(parameters:\(String,String\)*\):StandaloneWSRequest.this.Self) (same behaviour) or [`WsRequest.withQueryStringParameters`](https://playframework.com/documentation/2.6.x/api/scala/index.html#play.api.libs.ws.WSRequest@withQueryStringParameters\(parameters:\(String,String\)*\):WSRequest.this.Self) (throws away existing query string)

### Java

In Java, the `play.libs.ws.WS` class has been deprecated.  An injected `WSClient` instance should be used instead.

```java
public class MyService {
     private final WSClient ws;

     @Inject
     public MyService(WSClient ws) {
         this.ws = ws;
     }

     public void call() {     
         ws.url("http://localhost:9000/foo").get();
     }
}
```

If you cannot use an injected WSClient instance, then you can also create your [[own instance of WSClient|JavaWS#using-wsclient]], but you are then responsible for managing the lifecycle of the client.

If you are running a functional test, you can use the `play.test.WsTestClient`, which will start up and shut down a standalone `WSClient` instance:

```java
WSClient ws = play.test.WsTestClient.newClient(19001);
...
ws.close();
```

A normal `WSResponse` instance is returned from `stream()` instead of `StreamedResponse`.  You should call `response.getBodyAsSource()` to return the streamed result.
