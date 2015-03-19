<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Trust Stores and Key Stores

Trust stores and key stores contain X.509 certificates.  Those certificates contain public (or private) keys, and are organized and managed under either a TrustManager or a KeyManager, respectively.

If you need to generate X.509 certificates, please see [[Certificate Generation|CertificateGeneration]] for more information.

## Configuring a Trust Manager

A [trust manager](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#TrustManager) is used to keep trust anchors: these are root certificates which have been issued by certificate authorities.   It determines whether the remote authentication credentials (and thus the connection) should be trusted.  As an HTTPS client, the vast majority of requests will use only a trust manager.  

If you do not configure it at all, WS uses the default trust manager, which points to the `cacerts` key store in `${java.home}/lib/security/cacerts`.  If you configure a trust manager explicitly, it will override the default settings and the `cacerts` store will not be included.

If you wish to use the default trust store and add another store containing certificates, you can define multiple stores in your trust manager.  The [CompositeX509TrustManager](api/scala/index.html#play.api.libs.ws.ssl.CompositeX509TrustManager) will try each in order until it finds a match:

```
play.ws.ssl {
  trustManager = {
      stores = [
        { path: ${store.directory}/truststore.jks, type: "JKS" }  # Added trust store
        { path: ${java.home}/lib/security/cacerts, password = "changeit" } # Default trust store
      ]
  }
}
```


> **NOTE**: Trust stores should only contain CA certificates with public keys, usually JKS or PEM.  PKCS12 format is supported, but PKCS12 should not contain private keys in a trust store, as noted in the [reference guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SunJSSE).

## Configuring a Key Manager

A [key manager](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#KeyManager) is used to present keys for a remote host.  Key managers are typically only used for client authentication (also known as mutual TLS).

The [CompositeX509KeyManager](api/scala/index.html#play.api.libs.ws.ssl.CompositeX509KeyManager) may contain multiple stores in the same manner as the trust manager.

```
play.ws.ssl {
    keyManager = {
      stores = [
        {
          type: "pkcs12",
          path: "keystore.p12",
          password: "password1"
        },
      ]
    }
}
```

> **NOTE**: A key store that holds private keys should use PKCS12 format, as indicated in the [reference guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SunJSSE).

## Configuring a Store

A store corresponds to a [KeyStore](http://docs.oracle.com/javase/7/docs/api/java/security/KeyStore.html) object, which is used for both trust stores and key stores.  Stores may have a [type](http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyStore) -- `PKCS12`, [`JKS`](http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#KeystoreImplementation) or `PEM` (aka Base64 encoded DER certificate) -- and may have an associated password.

Stores must have either a `path` or a `data` attribute.  The `path` attribute must be a file system path.

```
{ type: "PKCS12", path: "/private/keystore.p12" }
```

The `data` attribute must contain a string of PEM encoded certificates.

```
{
  type: "PEM", data = """
-----BEGIN CERTIFICATE-----
...certificate data
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
...certificate data
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
...certificate data
-----END CERTIFICATE-----
"""
}
```

## Debugging

To debug the key manager / trust manager, set the following flags:

```
play.ws.ssl.debug = {
  ssl = true
  trustmanager = true
  keymanager = true
}
```

## Further Reading

In most cases, you will not need to do extensive configuration once the certificates are installed.  If you are having difficulty with configuration, the following blog posts may be useful:

* [Key Management](http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#KeyManagement)
* [Java 2-way TLS/SSL (Client Certificates) and PKCS12 vs JKS KeyStores](http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/)
* [HTTPS with Client Certificates on Android](http://chariotsolutions.com/blog/post/https-with-client-certificates-on/)
