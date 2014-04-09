<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Debugging SSL Connections

In the event that an HTTPS connection does not go through, debugging JSSE can be a hassle.

WS SSL provides configuration options that will turn on JSSE debug options defined in the [Debugging Utilities](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#Debug) and  [Troubleshooting Security](http://docs.oracle.com/javase/7/docs/technotes/guides/security/troubleshooting-security.html) pages.

To configure, set the `ws.ssl.debug` property in `application.conf`:

```
ws.ssl.debug = [
 "certpath", "ocsp",

 # "all "  # defines all of the below
 "ssl",
 "defaultctx",
 "handshake",
   "verbose",
   "data",
 "keygen",
 "keymanager",
 "pluggability",
 "record",
   "packet",
   "plaintext",
 "session",
 "sessioncache",
 "sslctx",
 "trustmanager"
]
```

> NOTE: This is a global setting that changes everything on the JVM, and is only intended for use in development environments.

## Further reading

Oracle has a number of sections on debugging JSSE issues:

* [Debugging SSL/TLS connections](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/ReadDebug.html)
* [JSSE Debug Logging With Timestamp](https://blogs.oracle.com/xuelei/entry/jsse_debug_logging_with_timestamp)
* [How to Analyze Java SSL Errors](http://www.smartjava.org/content/how-analyze-java-ssl-errors)
* [How this feature is implemented](http://tersesystems.com/2014/03/02/monkeypatching-java-classes/)