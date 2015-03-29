<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Configuring Certificate Validation

In an SSL connection, the identity of the remote server is verified using an X.509 certificate which has been signed by a certificate authority.

The JSSE implementation of X.509 certificates is defined in the [PKI Programmer's Guide](http://docs.oracle.com/javase/7/docs/technotes/guides/security/certpath/CertPathProgGuide.html).

Some X.509 certificates that are used by servers are old, and are using signatures that can be forged by an attacker.  Because of this, it may not be possible to verify the identity of the server if that signature algorithm is being used.  Fortunately, this is rare -- over 95% of trusted leaf certificates and 95% of trusted signing certificates use [NIST recommended key sizes](http://csrc.nist.gov/publications/nistpubs/800-131A/sp800-131A.pdf).

WS automatically disables weak signature algorithms and weak keys for you, according to the [current standards](http://sim.ivi.co/2012/04/nist-security-strength-time-frames.html).

This feature is similar to [jdk.certpath.disabledAlgorithms](http://sim.ivi.co/2013/11/harness-ssl-and-jsse-key-size-control.html), but is specific to the WS client and can be set dynamically, whereas jdk.certpath.disabledAlgorithms is global across the JVM, must be set via a security property, and is only available in JDK 1.7 and later.

You can override this to your tastes, but it is recommended to be at least as strict as the defaults.  The appropriate signature names can be looked up in the [Providers Documentation](http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html).

## Disabling Certificates with Weak Signature Algorithms

The default list of disabled signature algorithms is defined below:

```
play.ws.ssl.disabledSignatureAlgorithms = "MD2, MD4, MD5"
```

MD5 is disabled, based on the proven [collision attack](http://www.win.tue.nl/hashclash/rogue-ca/) and the Mozilla recommendations:

> MD5 certificates may be compromised when attackers can create a fake cert that hashes to the same value as one with a legitimate signature, and is hence trusted. Mozilla can mitigate this potential vulnerability by turning off support for MD5-based signatures. The MD5 root certificates don't necessarily need to be removed from NSS, because the signatures of root certificates are not validated (roots are self-signed). Disabling MD5 will impact intermediate and end entity certificates, where the signatures are validated.
>
> The relevant CAs have confirmed that they stopped issuing MD5 certificates. However, there are still many end entity certificates that would be impacted if support for MD5-based signatures was turned off in 2010. Therefore, we are hoping to give the affected CAs time to react, and are proposing the date of June 30, 2011 for turning off support for MD5-based signatures. The relevant CAs are aware that Mozilla will turn off MD5 support earlier if needed.

SHA-1 is considered weak, and new certificates should use digest algorithms from the [SHA-2 family](https://en.wikipedia.org/wiki/SHA-2).  However, old certificates should still be considered valid.

## Disabling Certificates With Weak Key Sizes

WS defines the default list of weak key sizes as follows:

```
play.ws.ssl.disabledKeyAlgorithms = "DHE keySize < 2048, ECDH keySize < 2048, ECDHE keySize < 2048, RSA keySize < 2048, DSA keySize < 2048, EC keySize < 224"
```

These settings are based in part on [keylength.com](https://keylength.com), and in part on the Mozilla recommendations:

> The NIST recommendation is to discontinue 1024-bit RSA certificates by December 31, 2010. Therefore, CAs have been advised that they should not sign any more certificates under their 1024-bit roots by the end of this year.
>
> The date for disabling/removing 1024-bit root certificates will be dependent on the state of the art in public key cryptography, but under no circumstances should any party expect continued support for this modulus size past December 31, 2013. As mentioned above, this date could get moved up substantially if new attacks are discovered. We recommend all parties involved in secure transactions on the web move away from 1024-bit moduli as soon as possible.

**NOTE:** because weak key sizes also apply to root certificates (which is not included in the certificate chain available to the PKIX certpath checker included in JSSE), setting this option will check the accepted issuers in any configured trustmanagers and keymanagers, including the default.

Over 95% of trusted leaf certificates and 95% of trusted signing certificates use [NIST recommended key sizes](http://csrc.nist.gov/publications/nistpubs/800-131A/sp800-131A.pdf), so this is considered a safe default.

## Disabling Weak Certificates Globally

To disable signature algorithms and weak key sizes globally across the JVM, use the `jdk.certpath.disabledAlgorithms` [security property](http://sim.ivi.co/2011/07/java-se-7-release-security-enhancements.html).  Setting security properties is covered in more depth in [[Configuring Cipher Suites|CipherSuites]] section.

> **NOTE** if configured, the `jdk.certpath.disabledAlgorithms` property should contain the settings from both `disabledKeyAlgorithms` and `disabledSignatureAlgorithms`.

## Debugging Certificate Validation

To see more details on certificate validation, set the following debug configuration:

```
play.ws.ssl.debug.certpath = true
```

The undocumented setting `-Djava.security.debug=x509` may also be helpful.

## Further Reading

* [Dates for Phasing out MD5-based signatures and 1024-bit moduli](https://wiki.mozilla.org/CA:MD5and1024)
* [Fixing X.509 Certificates](http://tersesystems.com/2014/03/20/fixing-x509-certificates/)
