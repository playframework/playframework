<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Server Backends

Play comes two configurable server backends, which handle the low level work of processing HTTP requests and responses to and from TCP/IP packets.

Starting in 2.6.x, the default server backend is the Akka HTTP server backend, based on the [Akka-HTTP](https://doc.akka.io/docs/akka-http/current/) server.  Prior to 2.6.x, the server backend is Netty.

* [[Akka HTTP Server|AkkaHttpServer]]
* [[Netty Server|NettyServer]]
