<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Trust Stores and Key Stores

Trust stores and key stores contain X.509 certificates.  Those certificates contain public (or private) keys, and are organized and managed under either a TrustManager or a KeyManager, respectively.

If you need to generate X.509 certificates, please see [[Certificate Generation|CertificateGeneration]] for more information.

## Configuring a Trust Manager

A [trust manager](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#TrustManager) is used to keep trust anchors: these are root certificates which have been issued by certificate authorities.   It determines whether the remote authentication credentials (and thus the connection) should be trusted.  As an HTTPS client, the vast majority of requests will use only a trust manager.  Trust stores should only contain public keys.

If you do not configure it at all, WS uses the default trust manager.  This points to the `cacerts` key store in `${java.home}/lib/security/cacerts`.

If you wish to use the default trust store and add another store containing certificates, you can define multiple stores in your trust manager.  The trust manager will try each in order until it finds a match:

```
ws.ssl {
  trustManager = {
      stores = [
        { path: ${store.directory}/truststore.jks, type: "JKS" }  # Added trust store
        { path: ${java.home}/lib/security/cacerts } # Default trust store
      ]
  }
}
```

If you configure a single store, the default `cacerts` store will not be included.

> NOTE: For a trust manager, if you are intending to use client authentication, you **must** use a store in the JKS format containing only the CA certificate for the client certs.  The reason is that the SunJSSE provider is [very limited](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#SunJSSE): "Storing trusted anchors in PKCS12 is not supported. Users should store trust anchors in JKS format and save private keys in PKCS12 format."

## Configuring a Key Manager

A [key manager](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#KeyManager) is used to present keys for a remote host.  Key managers are typically only used for client authentication (also known as mutual TLS).

The key manager may contain multiple stores in the same manner as the trust manager.

```
ws.ssl {
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

As recommended in the note, private keys should be stored in PKCS12 format in the key manager.

## Configuring a Key Store

A key store corresponds to a [KeyStore](http://docs.oracle.com/javase/7/docs/api/java/security/KeyStore.html) object.  Stores may have a [type](http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyStore) (`PKCS12`, [`JKS`](http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#KeystoreImplementation) or `PEM` (aka Base64 encoded DER certificate)) and may have an associated password.

Stores must have either a `path` or a `data` attribute.  The `path` attribute must be a file system path.

```
{ type: "PKCS12", path: "/private/keystore.p12" }
```

The `data` attribute must contain a string, and is only useful for PEM encoded certificates.  Currently, only public keys in PEM format are supported.

```
{
  type: "PEM", data = """
-----BEGIN CERTIFICATE-----
...certificate data
-----END CERTIFICATE-----
"""
}
```

## Debugging

To debug the key manager / trust manager, set the following flags:

```
ws.ssl.debug = [ "ssl", "trustmanager", "keymanager" ]
```

## Further Reading

In most cases, you will not need to do extensive configuration once the certificates are installed.  If you are having difficulty with configuration, the following blog posts may be useful:

* [Key Management](http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#KeyManagement)
* [Java 2-way TLS/SSL (Client Certificates) and PKCS12 vs JKS KeyStores](http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/)
* [HTTPS with Client Certificates on Android](http://chariotsolutions.com/blog/post/https-with-client-certificates-on/)

> **Next:** [[Configuring Protocols|Protocols]]
