# Quick Start to WS SSL

This section is for people who need to connect to a remote web service over HTTPS, and don't want to read through the entire manual.  If you need to set up a web service or configure client authentication, please proceed to the [[next section|CertificateGeneration]].

## Connecting to a Remote Server over HTTPS

If the remote server is using a certificate that is signed by a well known certificate authority, then WS should work out of the box without any additional configuration.  You're done!

If the web service is not using a well known certificate authority, then it is using either a private CA or a self-signed certificate.  You can determine this easily by using curl:

```
curl https://financialcryptography.com # uses cacert.org as a CA
```

If you receive the following error:

```
curl: (60) SSL certificate problem: Invalid certificate chain
More details here: http://curl.haxx.se/docs/sslcerts.html

curl performs SSL certificate verification by default, using a "bundle"
 of Certificate Authority (CA) public keys (CA certs). If the default
 bundle file isn't adequate, you can specify an alternate file
 using the --cacert option.
If this HTTPS server uses a certificate signed by a CA represented in
 the bundle, the certificate verification probably failed due to a
 problem with the certificate (it might be expired, or the name might
 not match the domain name in the URL).
If you'd like to turn off curl's verification of the certificate, use
 the -k (or --insecure) option.
```

Then you have to obtain the CA's certificate, and add it to the trust store.

## Obtain the Root CA Certificate

Ideally this should be done out of band: the owner of the web service should provide you with the root CA certificate directly, in a way that can't be faked, preferably in person.

In the case where there is no communication (and this is **not recommended**), you can sometimes get the root CA certificate directly from the certificate chain, using [`keytool from JDK 1.8`](http://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html):

```
keytool -printcert -sslserver playframework.com
```

which returns #2 as the root certificate:

```
Certificate #2
====================================
Owner: CN=GlobalSign Root CA, OU=Root CA, O=GlobalSign nv-sa, C=BE
Issuer: CN=GlobalSign Root CA, OU=Root CA, O=GlobalSign nv-sa, C=BE
```

To get the certificate chain in an exportable format, use the -rfc option:

```
keytool -printcert -sslserver playframework.com -rfc
```

which will return a series of certificates in PEM format:

```
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
```

which can be copied and pasted into a file.  The very last certificate in the chain will be the root CA certificate.

> **NOTE**: Not all websites will include the root CA certificate.  You should decode the certificate with keytool or with [certificate decoder](https://www.sslshopper.com/certificate-decoder.html) to ensure you have the right certificate.

## Point the trust manager at the PEM file

Add the following into `conf/application.conf`, specifying `PEM` format specifically:

```
play.ws.ssl {
  trustManager = {
    stores = [
      { type = "PEM", path = "/path/to/cert/globalsign.crt" }
    ]
  }
}
```

This will tell the trust manager to ignore the default `cacerts` store of certificates, and only use your custom CA certificate.

After that, WS will be configured, and you can test that your connection works with:

```
WS.url("https://example.com").get()
```

You can see more examples on the [[example configurations|ExampleSSLConfig]] page.
