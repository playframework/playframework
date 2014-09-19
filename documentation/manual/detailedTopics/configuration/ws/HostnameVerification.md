<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Hostname Verification

Hostname verification is a little known part of HTTPS that involves a [server identity check](http://tools.ietf.org/search/rfc2818#section-3.1) to ensure that the client is talking to the correct server and has not been redirected by a man in the middle attack.

The check involves looking at the certificate sent by the server, and verifying that the `dnsName` in the `subjectAltName` field of the certificate matches the host portion of the URL used to make the request.

WS SSL does hostname verification automatically, using the [DefaultHostnameVerifier](api/scala/index.html#play.api.libs.ws.ssl.DefaultHostnameVerifier) to implement the [hostname verifier](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#HostnameVerifier) fallback interface.

## Modifying the Hostname Verifier

If you need to specify a different hostname verifier, you can configure `application.conf` to provide your own custom [`HostnameVerifier`](http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/HostnameVerifier.html):

```
ws.ssl.hostnameVerifierClass=org.example.MyHostnameVerifier
```

## Debugging

Hostname Verification can be tested using `dnschef`.  A complete guide is out of scope of documentation, but an example can be found in [Testing Hostname Verification](http://tersesystems.com/2014/03/31/testing-hostname-verification/).

> NOTE: There is a known [bug](https://github.com/playframework/playframework/issues/2767) in Play that can result in silent timeouts when hostname verification fails.  This bug stems from an underlying AsyncHttpClient 1.8.x [issue](https://github.com/AsyncHttpClient/async-http-client/issues/555).  To see the hostname verification timeout, set `-Dlogger.org.jboss.netty=DEBUG` and look for `java.net.ConnectException: HostnameVerifier exception` in the logs.
>
> Usually the cause of hostname verification failure is an X.509 certificate with an invalid subjectAlternativeName.  Please check you are setting `-ext SAN="DNS:example.com"` when creating certificates -- see [[Certificate Generation|CertificateGeneration]] for more details.


## Further Reading

* [Fixing Hostname Verification](http://tersesystems.com/2014/03/23/fixing-hostname-verification/)

> **Next:** [[Example Configurations|ExampleSSLConfig]]
