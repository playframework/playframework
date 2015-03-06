# Generating X.509 Certificates

## X.509 Certificates

Public key certificates are a solution to the problem of identity.  Encryption alone is enough to set up a secure connection, but there's no guarantee that you are talking to the server that you think you are talking to.  Without some means to verify the identity of a remote server, an attacker could still present itself as the remote server and then forward the secure connection onto the remote server.  Public key certificates solve this problem.

The best way to think about public key certificates is as a passport system. Certificates are used to establish information about the bearer of that information in a way that is difficult to forge. This is why certificate verification is so important: accepting **any** certificate means that even an attacker's certificate will be blindly accepted.

## Using Keytool

keytool comes in several versions:

* [1.8](http://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
* [1.7](http://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html)
* [1.6](http://docs.oracle.com/javase/6/docs/technotes/tools/solaris/keytool.html)

The examples below use keytool 1.7, as 1.6 does not support the minimum required certificate extensions needed for marking a certificate for CA usage or for a hostname.

## Generating a random password

Create a random password using pwgen (`brew install pwgen` if you're on a Mac):

@[context](code/genpassword.sh)

## Server Configuration

You will need a server with a DNS hostname assigned, for hostname verification.  In this example, we assume the hostname is `example.com`.

### Generating a server CA

The first step is to create a certificate authority that will sign the example.com certificate.  The root CA certificate has a couple of additional attributes (ca:true, keyCertSign) that mark it explicitly as a CA certificate, and will be kept in a trust store.

@[context](code/genca.sh)

### Generating example.com certificates

The example.com certificate is presented by the `example.com` server in the handshake.

@[context](code/genserver.sh)

You should see:

```
Alias name: example.com
Creation date: ...
Entry type: PrivateKeyEntry
Certificate chain length: 2
Certificate[1]:
Owner: CN=example.com, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
Issuer: CN=exampleCA, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
```

> **NOTE**: Also see the [[Configuring HTTPS|ConfiguringHttps]] section for more information.

### Configuring example.com certificates in Nginx

If example.com does not use Java as a TLS termination point, and you are using nginx, you may need to export the certificates in PEM format.

Unfortunately, keytool does not export private key information, so openssl must be installed to pull private keys.

@[context](code/genserverexp.sh)

Now that you have both `example.com.crt` (the public key certificate) and `example.com.key` (the private key), you can set up an HTTPS server.

For example, to use the keys in nginx, you would set the following in `nginx.conf`:

```
ssl_certificate      /etc/nginx/certs/example.com.crt;
ssl_certificate_key  /etc/nginx/certs/example.com.key;
```

If you are using client authentication (covered in **Client Configuration** below), you will also need to add:

```
ssl_client_certificate /etc/nginx/certs/clientca.crt;
ssl_verify_client on;
```

You can check the certificate is what you expect by checking the server:

```
keytool -printcert -sslserver example.com
```

> **NOTE**: Also see the [[Setting up a front end HTTP server|HTTPServer]] section for more information.

## Client Configuration

There are two parts to setting up a client -- configuring a trust store, and configuring client authentication.

### Configuring a Trust Store

Any clients need to see that the server's example.com certificate is trusted, but don't need to see the private key.  Generate a trust store which contains only the certificate and hand that out to clients.  Many java clients prefer to have the trust store in JKS format.

@[context](code/gentruststore.sh)

You should see a `trustedCertEntry` for exampleca:

```
Alias name: exampleca
Creation date: ...
Entry type: trustedCertEntry

Owner: CN=exampleCA, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
Issuer: CN=exampleCA, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
```

The `exampletrust.jks` store will be used in the TrustManager.

```
play.ws.ssl {
  trustManager = {
    stores = [
      { path = "/Users/wsargent/work/ssltest/conf/exampletrust.jks" }
    ]
  }
}
```

> **NOTE**: Also see the [[Configuring Key Stores and Trust Stores|KeyStores]] section for more information.

### Configure Client Authentication

Client authentication can be obscure and poorly documented, but it relies on the following steps:

1. The server asks for a client certificate, presenting a CA that it expects a client certificate to be signed with.  In this case, `CN=clientCA` (see the [debug example](http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/ReadDebug.html)).
2. The client looks in the KeyManager for a certificate which is signed by `clientCA`, using `chooseClientAlias` and `certRequest.getAuthorities`.
3. The KeyManager will return the `client` certificate to the server.
4. The server will do an additional ClientKeyExchange in the handshake.

The steps to create a client CA and a signed client certificate are broadly similiar to the server certificate generation, but for convenience are presented in a single script:

@[context](code/genclient.sh)

There should be one alias `client`, looking like the following:

```
Your keystore contains 1 entry

Alias name: client
Creation date: ...
Entry type: PrivateKeyEntry
Certificate chain length: 2
Certificate[1]:
Owner: CN=client, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
Issuer: CN=clientCA, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US
```

And put `client.jks` in the key manager:

```
play.ws.ssl {
  keyManager = {
    stores = [
      { type = "JKS", path = "conf/client.jks", password = $PW }
    ]
  }
}
```

> **NOTE**: Also see the [[Configuring Key Stores and Trust Stores|KeyStores]] section for more information.

## Certificate Management Tools

If you want to examine certificates in a graphical tool than a command line tool, you can use [Keystore Explorer](http://keystore-explorer.sourceforge.net/) or [xca](http://sourceforge.net/projects/xca/).  [Keystore Explorer](http://keystore-explorer.sourceforge.net/) is especially convenient as it recognizes JKS format.  It works better as a manual installation, and requires some tweaking to the export policy.

If you want to use a command line tool with more flexibility than keytool, try [java-keyutil](https://code.google.com/p/java-keyutil/), which understands multi-part PEM formatted certificates and JKS.

## Certificate Settings

### Secure

If you want the best security, consider using [ECDSA](http://blog.cloudflare.com/ecdsa-the-digital-signature-algorithm-of-a-better-internet) as the signature algorithm (in keytool, this would be `-sigalg EC`). ECDSA is also known as "ECC SSL Certificate".

### Compatible

For compatibility with older systems, use RSA with 2048 bit keys and SHA256 as the signature algorithm.  If you are creating your own CA certificate, use 4096 bits for the root.

## Further Reading

* [JSSE Reference Guide To Creating KeyStores](http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#CreateKeystore)
* [Java PKI Programmer's Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/certpath/CertPathProgGuide.html)
* [Fixing X.509 Certificates](http://tersesystems.com/2014/03/20/fixing-x509-certificates/)
