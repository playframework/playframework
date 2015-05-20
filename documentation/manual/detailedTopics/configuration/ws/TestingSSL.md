<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Testing SSL

Testing an SSL client not only involves unit and integration testing, but also involves adversarial testing, which tests that an attacker cannot break or subvert a secure connection.

## Unit Testing

Play comes with `play.api.test.WsTestClient`, which provides two methods, `wsCall` and `wsUrl`.  It can be helpful to use `PlaySpecification` and `in new WithApplication` 

```
"calls index" in new WithApplication() {
  await(wsCall(routes.Application.index()).get())	
}
```

```
wsUrl("https://example.com").get()
```



## Integration Testing

If you want confirmation that your client is correctly configured, you can call out to [HowsMySSL](https://www.howsmyssl.com/s/api.html), which has an API to check JSSE settings.

@[context](code/HowsMySSLSpec.scala)

Note that if you are writing tests that involve custom configuration such as revocation checking or disabled algorithms, you may need to pass system properties into SBT:

```
javaOptions in Test ++= Seq("-Dcom.sun.security.enableCRLDP=true", "-Dcom.sun.net.ssl.checkRevocation=true", "-Djavax.net.debug=all")
```

## Adversarial Testing

There are several points of where a connection can be attacked.  Writing these tests is fairly easy, and running these adversarial tests against unsuspecting programmers can be extremely satisfying.  

> **NOTE**:This should not be taken as a complete list, but as a guide.  In situations where security is paramount, a review should be done by professional info-sec consultants.

### Testing Certificate Verification

Write a test to connect to "https://example.com".  The server should present a certificate which says the subjectAltName is dnsName, but the certificate should be signed by a CA certificate which is not in the trust store.  The client should reject it.

This is a very common failure.  There are a number of proxies like [mitmproxy](https://mitmproxy.org) or [Fiddler](http://www.telerik.com/fiddler) which will only work if certificate verification is disabled or the proxy's certificate is explicitly added to the trust store.

### Testing Weak Cipher Suites

The server should send a cipher suite that includes NULL or ANON cipher suites in the handshake.  If the client accepts it, it is sending unencrypted data.

> **NOTE**: For a more in depth test of a server's cipher suites, see [sslyze](https://github.com/iSECPartners/sslyze).

### Testing Certificate Validation

To test for weak signatures, the server should send the client a certificate which has been signed with, for example, the MD2 digest algorithm.  The client should reject it as being too weak.  

To test for weak certificate, The server should send the client a certificate which contains a public key with a key size under 1024 bits.  The client should reject it as being too weak.

> **NOTE**: For a more in depth test of certification validation, see [tlspretense](https://github.com/iSECPartners/tlspretense) and [frankencert](https://github.com/sumanj/frankencert).

### Testing Hostname Verification

Write a test to "https://example.com".  If the server presents a certificate where the subjectAltName's dnsName is not example.com, the connection should terminate.

> **NOTE**: For a more in depth test, see [dnschef](https://tersesystems.com/2014/03/31/testing-hostname-verification/). 

