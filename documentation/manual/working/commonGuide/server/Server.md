<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Server Backends

Play comes with two configurable server backends, which handle the low level work of processing HTTP requests and responses to and from TCP/IP packets.

Starting in 2.6.x, the default server backend is the Pekko HTTP server backend, based on the [Pekko-HTTP](https://pekko.apache.org/docs/pekko-http/1.0/) server.  Prior to 2.6.x, the server backend is Netty.

* [[Pekko HTTP Server|PekkoHttpServer]]
* [[Netty Server|NettyServer]]
