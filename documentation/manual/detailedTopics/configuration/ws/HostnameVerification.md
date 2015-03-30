<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Hostname Verification

Hostname verification is a little known part of HTTPS that involves a [server identity check](http://tools.ietf.org/search/rfc2818#section-3.1) to ensure that the client is talking to the correct server and has not been redirected by a man in the middle attack.

The check involves looking at the certificate sent by the server, and verifying that the `dnsName` in the `subjectAltName` field of the certificate matches the host portion of the URL used to make the request.

WS SSL does hostname verification automatically, using the [DefaultHostnameVerifier](api/scala/index.html#play.api.libs.ws.ssl.DefaultHostnameVerifier) to implement the [hostname verifier](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#HostnameVerifier) fallback interface.

## Modifying the Hostname Verifier

If you need to specify a different hostname verifier, you can configure `application.conf` to provide your own custom [`HostnameVerifier`](http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/HostnameVerifier.html):

```
play.ws.ssl.hostnameVerifierClass=org.example.MyHostnameVerifier
```

## Debugging

Hostname Verification can be tested using `dnschef`.  A complete guide is out of scope of documentation, but an example can be found in [Testing Hostname Verification](http://tersesystems.com/2014/03/31/testing-hostname-verification/).

## Further Reading

* [Fixing Hostname Verification](http://tersesystems.com/2014/03/23/fixing-hostname-verification/)
