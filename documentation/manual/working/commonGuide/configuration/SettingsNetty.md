<!--- Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com> -->

# Configuring Netty Server Backend

The Netty server backend is built on top of [Netty](https://netty.io/).

> **Note**: The Netty server backend is not the default in 2.6.x, and so must be specifically enabled. See more information in [[Netty Server|NettyServer]] documentation.

## Default configuration

Play uses the following default configuration:

@[](/confs/play-netty-server/reference.conf)

The configurations above are specific to Netty server backend, but other more generic configurations are also available:
 
@[](/confs/play-server/reference.conf)

## Configuring transport socket

Native socket transport has higher performance and produces less garbage and is available on Linux, macOS, FreeBSD and OpenBSD. You can configure the transport socket type in `application.conf`:

```properties
play.server {
  netty {
    transport = "native"
  }
}
```

When set to `native`, Play will automatically detect the operating system it is running on and load the appropriate native transport library.

> **Note**: On Windows, if the transport configuration is set to `native`, Play will ignore it and automatically fall back to Java NIO transport - just like when using the default `jdk` config.

## Configuring channel options

The available options are defined in the [Netty channel option documentation](https://netty.io/4.1/api/io/netty/channel/ChannelOption.html).

If you are using the native socket transport, you can set the following additional options:

- For Linux: [UnixChannelOption](https://netty.io/4.1/api/io/netty/channel/unix/UnixChannelOption.html) and [EpollChannelOption](https://netty.io/4.1/api/io/netty/channel/epoll/EpollChannelOption.html)
- For macOS/BSD: [UnixChannelOption](https://netty.io/4.1/api/io/netty/channel/unix/UnixChannelOption.html) and [KQueueChannelOption](https://netty.io/4.1/api/io/netty/channel/kqueue/KQueueChannelOption.html)
