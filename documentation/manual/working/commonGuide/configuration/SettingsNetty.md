<!--- Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring Netty Server Backend

The Netty server backend is built on top of [Netty](http://netty.io/).

> **NOTE**: The Netty server backend is not the default in 2.6.x, and so must be specifically enabled.

## Default configuration

Play uses the following default configuration:

@[](/confs/play-netty-server/reference.conf)

## Configuring transport socket

Native socket transport has higher performance and produces less garbage but is only available on Linux.
You can configure the transport socket type in `application.conf`:

```properties
play.server {
  netty {
    transport = "native"
  }
}
```

## Configuring channel options

The available options are defined in [Netty channel option documentation](http://netty.io/4.1/api/io/netty/channel/ChannelOption.html).
If you are using native socket transport you can set [additional options](http://netty.io/4.1/api/io/netty/channel/epoll/EpollChannelOption.html).
