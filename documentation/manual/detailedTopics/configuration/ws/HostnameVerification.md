<!--- Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Hostname Verification

Hostname verification is a little known part of HTTP that involves a [server identity check](http://tools.ietf.org/search/rfc2818#section-3.1) to ensure that the client is talking to the correct server and has not been redirected by a [Man in the Middle attack](http://tersesystems.com/2014/03/23/fixing-hostname-verification/).

The check involves looking at the certificate sent by the server, and verifying that the `dnsName` in the `subjectAltName` field of the certificate matches the host portion of the URL used to make the request.

## Modifying the Hostname Verifier

WS SSL does hostname verification automatically as part of the connection automatically, using a [hostname verifier](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html#HostnameVerifier).

If you need to specify a different hostname verifier, you can configure `application.conf` to provide your own custom `HostnameVerifier`:

```
ws.ssl.hostnameVerifierClassName=org.example.MyHostnameVerifier
```

## Generating a certificate that passes hostname verification

It is fairly easy to make a self signed certificate which has the correct name, by using [keytool](http://docs.oracle.com/javase/7/docs/technotes/tools/windows/keytool.html) with the `-ext` extension:

```
keytool -genkeypair \
   -keystore keystore.jks \
  -dname "CN=example.com, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US" \
  -keypass changeit \
  -storepass changeit \
  -keyalg RSA \
  -keysize 2048 \
  -alias example \
  -ext SAN=DNS:example.com \
  -validity 9999
```

You can then examine the certificate with:

```
keytool -list -v -alias example -storepass changeit -keystore keystore.jks
```

And you will see:

```
Alias name: example
Creation date: Mar 26, 2014
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=example.com, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
Issuer: CN=example.com, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
Serial number: 4180f5e0
Valid from: Wed Mar 26 10:22:59 PDT 2014 until: Thu Mar 26 10:22:59 PDT 2015
Certificate fingerprints:
   MD5:  F3:32:40:C9:00:59:D3:32:E1:75:85:7A:A9:68:6D:F5
   SHA1: 37:9D:90:44:AB:41:AD:8D:F5:E4:6C:03:5F:22:61:53:EF:23:67:1E
   SHA256: 88:FF:83:43:E1:2D:F1:19:7B:3E:1D:4D:88:40:C3:8C:8A:96:2D:75:16:4F:C8:E9:0B:99:F5:0E:53:4A:C1:17
   Signature algorithm name: SHA256withRSA
   Version: 3

Extensions:

#1: ObjectId: 2.5.29.17 Criticality=false
SubjectAlternativeName [
  DNSName: example.com
]

#2: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 62 30 8E 8C F2 7C 7A BC   FD EB AC 75 F6 BD FD F1  b0....z....u....
0010: 3E 73 D5 A9                                        >s..
]
]
```

## Disabling Hostname Verification

If you need to disable hostname verification -- which in practice is not necessary and leads to far more trouble than generating a certificate with the right name -- you can set a loose flag:

```
ws.ssl.loose.disableHostnameVerification=true
```