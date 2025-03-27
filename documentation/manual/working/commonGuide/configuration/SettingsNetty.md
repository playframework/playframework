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

[Native socket transport](https://netty.io/wiki/native-transports.html) has higher performance and produces less garbage and is available on Linux, macOS, FreeBSD and OpenBSD. You can configure the transport socket type in `application.conf`:

```properties
play.server {
  netty {
    transport = "native"
  }
}
```

In addition to `native`, you can set transport to `io_uring` to make use of [Netty's io_uring native transport](https://github.com/netty/netty/tree/4.2/transport-native-io_uring). Be aware `io_uring` is available on Linux only.

When set to `native` or `io_uring`, Play will automatically detect the operating system it is running on and load the appropriate native transport library.

> **Note**: On Windows, if the transport configuration is set to `native` or `io_uring`, Play will ignore it and automatically fall back to Java NIO transport - just like when using the default `jdk` config. A similiar fallback mechanism kicks in on macOS: If transport is set to `io_uring`, Play will fall back to `native` instead, because the `io_uring` native transport is not available on macOS - but on Linux only (using Kernel 5.14 or newer compiled with [`CONFIG_IO_URING=y`](https://github.com/torvalds/linux/blob/v6.14/init/Kconfig#L1739-L1746) - which is the default anyway).

## Configuring channel options

The available options are defined in the [Netty channel option documentation](https://netty.io/4.2/api/io/netty/channel/ChannelOption.html).

If you are using the native socket transport, you can set the following additional options:

- For Linux: [UnixChannelOption](https://netty.io/4.2/api/io/netty/channel/unix/UnixChannelOption.html) and [EpollChannelOption](https://netty.io/4.2/api/io/netty/channel/epoll/EpollChannelOption.html)
    - When using `io_uring`: [IoUringChannelOption](https://netty.io/4.2/api/io/netty/channel/uring/IoUringChannelOption.html) (instead of `EpollChannelOption`)
- For macOS/BSD: [UnixChannelOption](https://netty.io/4.2/api/io/netty/channel/unix/UnixChannelOption.html) and [KQueueChannelOption](https://netty.io/4.2/api/io/netty/channel/kqueue/KQueueChannelOption.html)
