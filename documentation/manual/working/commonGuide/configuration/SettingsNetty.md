<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Configuring netty

Play 2's main server is built on top of [Netty](http://netty.io/).

## Default configuration

Play uses the following default configuration:

@[](/confs/play-netty-server/reference.conf)

## Configuring transport socket

Native socket transport has higher performance and produces less garbage but are only available on linux 
You can configure the transport socket type in `application.conf`:

```properties
play.server {
  netty {
    transport = "native"
  }
}
```

## Configuring channel options

The available options are defined in [Netty channel option documentation](http://netty.io/4.0/api/io/netty/channel/ChannelOption.html).
If you are using native socket transport you can set [additional options](http://netty.io/4.0/api/io/netty/channel/epoll/EpollChannelOption.html).
