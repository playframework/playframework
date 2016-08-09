<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play WS Migration Guide

## Scala

The deprecated Scala singleton object `play.api.libs.ws.WS` has been removed.  An instance of `WSClient` should be used instead.  

For Guice, there is a `WSClient` available in the system:

``` scala
class MyService @Inject()(ws: WSClient) {
   def call(): Unit = {     
     ws.url("http://localhost:9000/foo").get()
   }
}
```

If you cannot use an injected WSClient instance, then you can also create your [[own instance of WSClient|ScalaWS#using-wsclient]], but you are then responsible for managing the lifecycle of the client.

If you are running a functional test, you can use the `play.api.test.WsTestClient`, which will start up and shut down a standalone WSClient instance:

``` scala
play.api.test.WsTestClient.withClient { ws =>
  ws.url("http://localhost:9000/foo").get()
}
```

## Java

In Java, the `play.libs.ws.WS` class has been deprecated.  An injected `WSClient` instance should be used instead.

``` java
class MyService {
     private final WSClient ws;

     @Inject
     public MyService(ws: WSClient) {
         this.ws = ws;
     }

     public void call() {     
         ws.url("http://localhost:9000/foo").get();
     }
}
```

If you cannot use an injected WSClient instance, then you can also create your [[own instance of WSClient|JavaWS#using-wsclient]], but you are then responsible for managing the lifecycle of the client.

If you are running a functional test, you can use the `play.test.WsTestClient`, which will start up and shut down a standalone `WSClient` instance:

``` java
WSClient ws = play.test.WsTestClient.newClient(19001);
...
ws.close();
```



