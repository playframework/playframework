<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Certificate Revocation

Certificate Revocation in JSSE can be done through two means: certificate revocation lists (CRLs) and OCSP.

Certificate Revocation can be very useful in situations where a server's private keys are compromised, as in the case of [Heartbleed](http://heartbleed.com).

Certificate Revocation is disabled by default in JSSE.  It is defined in two places:

* [PKI Programmer's Guide, Appendix C](http://docs.oracle.com/javase/6/docs/technotes/guides/security/certpath/CertPathProgGuide.html#AppC)
* [Enable OCSP Checking](https://blogs.oracle.com/xuelei/entry/enable_ocsp_checking)

To enable OCSP, you must set the following system properties on the command line:

```
java -Dcom.sun.security.enableCRLDP=true -Dcom.sun.net.ssl.checkRevocation=true
```

After doing the above, you can enable certificate revocation in the client:

```
play.ws.ssl.checkRevocation = true
```

Setting `checkRevocation` will set the internal `ocsp.enable` security property automatically:

```
java.security.Security.setProperty("ocsp.enable", "true")
```

And this will set OCSP checking when making HTTPS requests.

> NOTE: Enabling OCSP requires a round trip to the OCSP responder.  This adds a notable overhead on HTTPS calls, and can make calls up to [33% slower](http://blog.cloudflare.com/ocsp-stapling-how-cloudflare-just-made-ssl-30).  The mitigation technique, OCSP stapling, is not supported in JSSE.

Or, if you wish to use a static CRL list, you can define a list of URLs:

```
play.ws.ssl.revocationLists = [ "http://example.com/crl" ]
```

## Debugging

To test certificate revocation is enabled, set the following options:

```
play.ws.ssl.debug = {
 certpath = true
 ocsp = true
}
```

And you should see something like the following output:

```
certpath: -Using checker7 ... [sun.security.provider.certpath.RevocationChecker]
certpath: connecting to OCSP service at: http://gtssl2-ocsp.geotrust.com
certpath: OCSP response status: SUCCESSFUL
certpath: OCSP response type: basic
certpath: Responder's name: CN=GeoTrust SSL CA - G2 OCSP Responder, O=GeoTrust Inc., C=US
certpath: OCSP response produced at: Wed Mar 19 13:57:32 PDT 2014
certpath: OCSP number of SingleResponses: 1
certpath: OCSP response cert #1: CN=GeoTrust SSL CA - G2 OCSP Responder, O=GeoTrust Inc., C=US
certpath: Status of certificate (with serial number 159761413677206476752317239691621661939) is: GOOD
certpath: Responder's certificate includes the extension id-pkix-ocsp-nocheck.
certpath: OCSP response is signed by an Authorized Responder
certpath: Verified signature of OCSP Response
certpath: Response's validity interval is from Wed Mar 19 13:57:32 PDT 2014 until Wed Mar 26 13:57:32 PDT 2014
certpath: -checker7 validation succeeded
```

## Further Reading

* [Fixing Certificate Revocation](http://tersesystems.com/2014/03/22/fixing-certificate-revocation/)
