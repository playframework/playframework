<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Embedding a Play server in your application

While Play apps are most commonly used as their own container, you can also embed a Play server into your own existing application.  This can be used in conjunction with the Twirl template compiler and Play routes compiler, but these are of course not necessary, a common use case for embedding a Play application will be because you only have a few very simple routes.

The simplest way to start an embedded Play server is to use the [`Server`](api/java/play/server/Server.html) factory methods.  If all you need to do is provide some straight forward routes, you may decide to use the [[Routing DSL|JavaRoutingDsl]], so you will need the following imports:

@[imports](code/javaguide/advanced/embedding/JavaEmbeddingPlay.java)

Then you can create a server using the `forRouter` method:

@[simple](code/javaguide/advanced/embedding/JavaEmbeddingPlay.java)

By default, this will start a server on a random port in test mode.  You can check the port using the `httpPort` method:

@[http-port](code/javaguide/advanced/embedding/JavaEmbeddingPlay.java)

You can configure the server by passing in a `port` and/or `mode`:

@[config](code/javaguide/advanced/embedding/JavaEmbeddingPlay.java)

To stop the server once you've started it, simply call the `stop` method:

@[stop](code/javaguide/advanced/embedding/JavaEmbeddingPlay.java)

> **Note:** Play requires an application secret to be configured in order to start.  This can be configured by providing an `application.conf` file in your application, or using the `play.crypto.secret` system property.