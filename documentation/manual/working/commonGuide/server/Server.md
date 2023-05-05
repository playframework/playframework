<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Server Backends

Play comes with two configurable server backends, which handle the low level work of processing HTTP requests and responses to and from TCP/IP packets.

Starting in 2.6.x, the default server backend is the Akka HTTP server backend, based on the [Akka-HTTP](https://doc.akka.io/docs/akka-http/10.2/) server.  Prior to 2.6.x, the server backend is Netty.

* [[Akka HTTP Server|AkkaHttpServer]]
* [[Netty Server|NettyServer]]
