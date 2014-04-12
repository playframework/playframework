# Example Configurations

TLS can be very confusing.  Here are some settings that can help.

## Connecting to an internal Web Service

If you are using WS to communicate with a single internal web service which is configured with an up to date TLS implementation, then you have no need to use an external CA.  Internal certificates will work fine, and are arguably [more secure](http://www.thoughtcrime.org/blog/authenticity-is-broken-in-ssl-but-your-app-ha/) than the CA system.

Generate a self signed certificate from the [[generating certificates|CertificateGeneration]] section, and tell the client to trust the CA's public certificate.

```
ws.ssl {
  trustManager = {
    stores = [
      { type = "PEM", path = "exampleca.crt" }
    ]
  }
}
```

## Connecting to several external web services

If you are communicating with several external web services, then you may find it more convenient to configure one client with several stores:

```
ws.ssl {
  trustManager = {
    stores = [
      { type = "PEM", path = "service1.pem" }
      { path = "service2.pks" }
      { path = "service3.pks" }
    ]
  }
}
```

If client authentication is required, then you can also set up the key manager with several stores:

```
ws.ssl {
    keyManager = {
    stores = [
      { type: "PKCS12", path: "keys/service1-client.p12", password: "changeit1" },
      { type: "PKCS12", path: "keys/service2-client.p12", password: "changeit2" },
      { type: "PKCS12", path: "keys/service3-client.p12", password: "changeit3" }
    ]
  }
}
```

## Compatibility Profile

If you are using WS to access a number of unknown servers or older servers, then you may wish to use a more accepting profile.

Enable the legacy hello messages for older systems:

```
-Dsun.security.ssl.allowLegacyHelloMessages=true
```

You may wish to set the system property:

```
-Dcom.sun.net.ssl.rsaPreMasterSecretFix=true
```
 
to correct a [version number problem](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html) in SSLv3 and TLS version 1.0.      

And configure the client as below:

```
ws.ssl {

  enabledProtocols = ["TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3"] 

  disabledSignatureAlgorithms = "MD2, MD4, MD5"

  disabledKeyAlgorithms = "RSA keySize < 1024, DSA keySize < 1024, EC keySize < 160"

  loose.allowWeakProtocols = true

  trustManager = {
    stores = [
      { path: ${store.directory}/truststore.jks }     # Added trust store
      { path: ${java.home}/lib/security/cacerts } # Fallback to default JSSE trust store
    ]
  }
}
```

## Debugging

If you want confirmation that your client is correctly configured, you can call out to [HowsMySSL](https://www.howsmyssl.com/s/api.html), which has an API to check JSSE settings.

```scala
class HowsMySSLSpec extends PlaySpecification {

  def createClient(rawConfig: play.api.Configuration): WSClient = {
    val parser = new DefaultWSConfigParser(rawConfig)
    val clientConfig = parser.parse()
    clientConfig.ssl.map {
      _.debug.map(new DebugConfiguration().configure)
    }
    val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
    val client = new NingWSClient(builder.build())
    client
  }

  def configToMap(configString: String): Map[String, _] = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseString(configString).root().unwrapped().asScala.toMap
  }

  "WS" should {

    "verify common behavior" in {
      val rawConfig = play.api.Configuration(ConfigFactory.parseString(
        """
          |ws.ssl.protocol="TLSv1.2"
        """.stripMargin))

      val client = createClient(rawConfig)
      val response = await(client.url("https://www.howsmyssl.com/a/check").get())(5.seconds)
      response.status must be_==(200)

      val jsonOutput = response.json
      val result = (jsonOutput \ "tls_version").validate[String]
      result must beLike {
        case JsSuccess(value, path) =>
          value must contain("TLS 1.2")
      }
    }
  }
}
```


Note that if you are writing tests that involve custom configuration such as revocation checking, you may need to pass system properties into SBT:

```
javaOptions in Test ++= Seq("-Dcom.sun.security.enableCRLDP=true", "-Dcom.sun.net.ssl.checkRevocation=true", "-Djavax.net.debug=all")
```

> **Next:** [[Using the Default SSLContext|DefaultContext]]
