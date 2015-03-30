# Example Configurations

TLS can be very confusing.  Here are some settings that can help.

## Connecting to an internal web service

If you are using WS to communicate with a single internal web service which is configured with an up to date TLS implementation, then you have no need to use an external CA.  Internal certificates will work fine, and are arguably [more secure](http://www.thoughtcrime.org/blog/authenticity-is-broken-in-ssl-but-your-app-ha/) than the CA system.

Generate a self signed certificate from the [[generating certificates|CertificateGeneration]] section, and tell the client to trust the CA's public certificate.

```
play.ws.ssl {
  trustManager = {
    stores = [
      { type = "JKS", path = "exampletrust.jks" }
    ]
  }
}
```

## Connecting to an internal web service with client authentication

If you are using client authentication, then you need to include a keyStore to the key manager that contains a PrivateKeyEntry, which consists of a private key and the X.509 certificate containing the corresponding public key.  See the "Configure Client Authentication" section in [[generating certificates|CertificateGeneration]].

```
play.ws.ssl {
  keyManager = {
    stores = [
      { type = "JKS", path = "client.jks", password = "changeit1" }
    ]
  }
  trustManager = {
    stores = [
      { type = "JKS", path = "exampletrust.jks" }
    ]
  }
}
```

## Connecting to several external web services

If you are communicating with several external web services, then you may find it more convenient to configure one client with several stores:

```
play.ws.ssl {
  trustManager = {
    stores = [
      { type = "PEM", path = "service1.pem" }
      { path = "service2.jks" }
      { path = "service3.jks" }
    ]
  }
}
```

If client authentication is required, then you can also set up the key manager with several stores:

```
play.ws.ssl {
    keyManager = {
    stores = [
      { type: "PKCS12", path: "keys/service1-client.p12", password: "changeit1" },
      { type: "PKCS12", path: "keys/service2-client.p12", password: "changeit2" },
      { type: "PKCS12", path: "keys/service3-client.p12", password: "changeit3" }
    ]
  }
}
```

## Both Private and Public Servers

If you are using WS to access both private and public servers on the same profile, then you will want to include the default JSSE trust store as well:

```
play.ws.ssl {
  trustManager = {
    stores = [
      { path: exampletrust.jks }     # Added trust store
      { path: ${java.home}/lib/security/cacerts } # Fallback to default JSSE trust store
    ]
  }
}
```


