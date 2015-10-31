<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Using the Default SSLContext

If you don't want to use the SSLContext that WS provides for you, and want to use `SSLContext.getDefault`, please set:

```
play.ws.ssl.default = true
```

## Debugging

If you want to debug the default context,

```
play.ws.ssl.debug {
  ssl = true
  sslctx = true
  defaultctx = true
}
```

If you are using the default SSLContext, then the only way to change JSSE behavior is through manipulating the [JSSE system properties](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#Customization).
