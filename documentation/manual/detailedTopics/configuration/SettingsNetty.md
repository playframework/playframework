<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
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

You need as well to add `netty-transport-native-epoll` as a dependency:

```scala
libraryDependencies ++= Seq(
    "io.netty" % "netty-transport-native-epoll" % "4.0.33.final" classifier "linux-x86_64"
)
```


## Configuring channel options

The available options are defined in [Netty channel option documentation](http://netty.io/4.0/api/io/netty/channel/ChannelOption.htm).
If you are using native socket transport you can set [additional options](http://netty.io/4.0/api/io/netty/channel/epoll/EpollChannelOption.html).