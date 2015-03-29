<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Protocols

By default, WS SSL will use the most secure version of the TLS protocol available in the JVM.

* On JDK 1.7 and later, the default protocol is "TLSv1.2".
* On JDK 1.6, the default protocol is "TLSv1".

The full protocol list in JSSE is available in the [Standard Algorithm Name Documentation](http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#jssenames).

## Defining the default protocol

If you want to define a different [default protocol](http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLContext.html#getInstance\(java.lang.String\)), you can set it specifically in the client:

```
# Passed into SSLContext.getInstance()
play.ws.ssl.protocol = "TLSv1.2"
```

If you want to define the list of enabled protocols, you can do so explicitly:

```
# passed into sslContext.getDefaultParameters().setEnabledProtocols()
play.ws.ssl.enabledProtocols = [
  "TLSv1.2",
  "TLSv1.1",
  "TLSv1"
]
```

If you are on JDK 1.8, you can also set the `jdk.tls.client.protocols` system property to enable client protocols globally.

WS recognizes "SSLv3", "SSLv2" and "SSLv2Hello" as weak protocols with a number of [security issues](https://www.schneier.com/paper-ssl.pdf), and will throw an exception if they are in the `ws.ssl.enabledProtocols` list.  Virtually all servers support `TLSv1`, so there is no advantage in using these older protocols.

## Debugging

The debug options for configuring protocol are:

```
play.ws.ssl.debug = {
  ssl = true
  sslctx = true
  handshake = true
  verbose = true
  data = true
}
```
