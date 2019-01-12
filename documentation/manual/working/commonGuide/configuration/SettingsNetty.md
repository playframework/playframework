<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring Netty Server Backend

The Netty server backend is built on top of [Netty](https://netty.io/).

> **Note**: The Netty server backend is not the default in 2.6.x, and so must be specifically enabled. See more information in [[Netty Server|NettyServer]] documentation.

## Default configuration

Play uses the following default configuration:

@[](/confs/play-netty-server/reference.conf)

The configurations above are specific to Netty server backend, but other more generic configurations are also available:
 
@[](/confs/play-server/reference.conf)

## Configuring transport socket

Native socket transport has higher performance and produces less garbage but is only available on Linux. You can configure the transport socket type in `application.conf`:

```properties
play.server {
  netty {
    transport = "native"
  }
}
```

## Configuring channel options

The available options are defined in [Netty channel option documentation](https://netty.io/4.1/api/io/netty/channel/ChannelOption.html). If you are using native socket transport you can set [additional options](https://netty.io/4.1/api/io/netty/channel/epoll/EpollChannelOption.html).
