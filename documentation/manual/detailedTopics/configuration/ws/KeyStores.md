<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Trust Stores and Key Stores

Trust stores and key stores can be defined in WS through `application.conf`.

## Configuring a Trust Manager

A [trust manager](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#TrustManager) is used to keep trust anchors: these are root certificates which have been issued by certificate authorities.   It determines whether the remote authentication credentials (and thus the connection) should be trusted.

If you do not configure it at all, WS uses the default trust manager.  This points to the `cacerts` key store in `${java.home}/lib/security/cacerts`.

If you wish to use the default trust store and add another store containing certificates, you can define multiple stores in your trust manager.  The trust manager will try each in order until it finds a match:


```
ws.ssl {
  trustManager = {
      stores = [
        { path: ${store.directory}/truststore }     # Added trust store
        { path: ${java.home}/lib/security/cacerts } # Default trust store
      ]
  }
}
```


## Configuring a Key Manager

A [key manager](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#KeyManager) is used to present keys for a remote host.  Key managers are typically only used for client authentication (also known as mutual TLS).

The key manager may contain multiple stores in the same manner as the trust manager.

```
ws.ssl {
    keyManager = {
      stores = [
        {
          type: "JKS",
          path: "keystore.jks",
          password: "password1"
        },
      ]
    }
}
```

## Configuring a Key Store

A key store corresponds to a [KeyStore](http://docs.oracle.com/javase/7/docs/api/java/security/KeyStore.html) object.  Stores may have a [type](http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyStore) (`PKCS12`, [`JKS`](http://docs.oracle.com/javase/7/docs/technotes/guides/security/crypto/CryptoSpec.html#KeystoreImplementation) or `PEM` (aka Base64 encoded DER certificate)) and may have an associated password.

Stores must have either a `path` or a `data` attribute.  The `path` attribute must be a file system path.

```
{ type: "PKCS12", path: "/private/keystore.p12" }
```

The `data` attribute must contain a string, and is only useful for PEM encoded certificates.

```
{
  type: "PEM", data = """
-----BEGIN CERTIFICATE-----
...certificate data
-----END CERTIFICATE-----
"""
}
```
