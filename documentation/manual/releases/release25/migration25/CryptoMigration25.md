<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Crypto Migration Guide

From Play 1.x, Play has come with a Crypto object that provides some cryptographic operations.  This used internally by Play.  The Crypto object is not mentioned in the documentation, but is mentioned as "cryptographic utilities" in the scaladoc:

"These utilities are intended as a convenience, however it is important to read each methods documentation and understand the concepts behind encryption to use this class properly.  Safe encryption is hard, and there is no substitute for an adequate understanding of cryptography.  These methods will not be suitable for all encryption needs."

For a variety of reasons, providing cryptographic utilities as a convenience has turned out not to be workable. In 2.5.x, the Play-specific functionality has been broken into `CookieSigner`, `CSRFTokenSigner` and `AESSigner` traits, and the `Crypto` singleton object deprecated.

The remainder of this document will discuss internal functionality behind Crypto, the suitability (and unsuitability) of cryptographic operations, and migration paths for users moving off Crypto functionality.

For more information about cryptography, we recommend reading the [OWASP Cryptographic Storage Cheatsheet](https://www.owasp.org/index.php/Cryptographic_Storage_Cheat_Sheet).

## Message Authentication

Play uses the `Crypto.sign` method to provide message authentication for session cookies.   There are several reasons why `Crypto.sign` should not be used outside the purpose of signing cookies: MAC algorithm independence, additional functionality, and potential misuse of HMAC as a password hashing algorithm.

### MAC Algorithm Independence

Play currently uses HMAC-SHA1 for signing and verifying session cookies.  An [HMAC](https://en.wikipedia.org/wiki/Hash-based_message_authentication_code) is a cryptographic function that authenticates that data has not been tampered with, using a secret key (the [[application secret|ApplicationSecret]] defined as play.crypto.secret) together with a message digest function (in this case [SHA-1](https://en.wikipedia.org/wiki/SHA-1)).  SHA-1 has suffered [some attacks recently](https://sites.google.com/site/itstheshappening/), but it remains secure when used with an HMAC for [message authenticity](https://www.killring.org/how-broken-is-sha-1).

Play needs to have the flexibility be able to move to a different HMAC function [as needed](http://valerieaurora.org/hash.html) and so, should not be part of the public API.

### Potential New Functionality

Play currently signs the session cookie, but does not add any session timeout or expiration date to the session cookie.  This means that, given the appropriate opening, an active attacker could swap out one session cookie for another one.  

Play may potentially add [session timeout functionality](https://github.com/google/keyczar/blob/master/java/code/src/org/keyczar/TimeoutSigner.java#L109) to Crypto.sign, which again would result in breaking user level functionality if this is marked as public API.

### Misuse as a Password Hash

Please do not use `Crypto.sign` or any kind of HMAC, as they are not designed for password hashing.  MACs are designed to be fast and cheap, while password hashing should be slow and expensive.  Please look at scrypt, bcrypt, or PBKDF2 -- [jBCrypt](http://www.mindrot.org/projects/jBCrypt/) in particular is well known as a bcrypt implementation in Java.

## Symmetric Encryption

Crypto contains two methods for symmetric encryption, `Crypto.encryptAES` and `Crypto.decryptAES`. These methods are not used internally by Play, but [significant](https://github.com/playframework/playframework/issues/4407) [developer](https://groups.google.com/d/msg/play-framework-dev/Rlrt89Ky_Rk/j6Iq6-snDw8J) [effort](https://groups.google.com/forum/#!topic/play-framework/Pao8MnADAqw) [has](https://ipsec.pl/play-framework/2014/session-variables-encryption-play-framework.html) gone into reviewing these methods.  These methods will be deprecated, and may be removed in future versions.

As alluded to in the warning, these methods are not generally "safe" -- there are some common modes of operation that are not secure using these methods.  Here follows a brief description of some cryptographic issues using `Crypto.encryptAES`.

Again, `Crypto.encryptAES` is never used directly in Play, so this isn’t a security vulnerability in Play itself.   

### Use of Stream Cipher without Authentication

`Crypto.encryptAES` is, by default, using AES-CTR, a mode of AES that provides encryption but does not provide authentication -- this means that an active attacker can swap out the contents of the encrypted text with something else.  This property, known as "malleability", means that it’s possible to [recover plaintext](https://news.ycombinator.com/item?id=639761) under certain conditions, and alter the message.  There are two constructions that are used to mitigate this: either the encrypted text is signed (known as “Encrypt Then MAC”) or authenticated encryption, such as AES-GCM, can be used.

Play uses `Crypto.encryptAES` to encrypt the contents of a session cookie (which has a MAC applied).  Since Play uses "Encrypt Then MAC", it is not vulnerable to attack -- however, users who are making use of symmetric encryption without a MAC are potentially vulnerable.

Users are encouraged not to implement their own Encrypt-Then-MAC construction.  Instead, the appropriate solution here is authenticated encryption, which both authenticates and encrypts data at the same time -- see the migration section for details.  

### Violation of Key Separation Principle

Although using AES with an HMAC is considered a secure construction, there is a problem in the way that Play uses the secret key for encryption.  The "key separation principle" says that you should only ever use one key for one purpose.  In this case, not only is play.crypto.secret is used for signing, but also encryption in `Crypto.encryptAES`.

For customers who are using `Crypto.encryptAES`, there is no immediate security vulnerability that results from the mixing of key usage here:

"With HMAC vs AES, no such interference is known. The *general feeling* of cryptographers is that AES and SHA-1 (or SHA-256) are "sufficiently different" that there should be no practical issue with using the same key for AES and HMAC/SHA-1." -- <https://crypto.stackexchange.com/a/8086>.

Once the application gets larger, the key separation principle might be also violated in another way: If `Crypto.encryptAES` is used for multiple purposes, using separate keys is also advised.

### Global configuration of mode

As mentioned above, AES can be used with various modes of operation. Using a different mode might require different additional security measures.

Play, however, offers configuring mode of operation globally by configuring the play.crypto.aes.transformation configuration option. That is, it influences whole application, including all libraries that use `Crypto.encryptAES`. As a result, it is hard to know exactly what the impact of changing the option on the whole application.

## Migration

There are several migration paths from Crypto functionality.  In order of preference, they are Kalium, Keyczar, or pure JCA.

### Kalium

If you have control over binaries in your production environment and do not have external requirements for NIST approved algorithms: use [Kalium](https://abstractj.github.io/kalium/), a wrapper over the [libsodium](https://download.libsodium.org/doc/) library.

If you need a MAC replacement for `Crypto.sign`, use `org.abstractj.kalium.keys.AuthenticationKey`, which implements HMAC-SHA512/256.

If you want a symmetric encryption replacement for `Crypto.encryptAES`, then use `org.abstractj.kalium.crypto.SecretBox`, which implements [secret-key authenticated encryption](https://download.libsodium.org/doc/secret-key_cryptography/authenticated_encryption.html).

Note that Kalium does require that a libsodium binary be [installed](https://download.libsodium.org/doc/installation/index.html), preferably from source that you have verified.

### Keyczar

If you are looking for a pure Java solution or depend on NIST approved algorithms, [Keyczar](https://tersesystems.com/2015/10/05/effective-cryptography-in-the-jvm/) provides a high level cryptographic library on top of JCA.  Note that Keyczar does not have the same level of support as libsodium / Kalium, and so Kalium is preferred.

If you need a MAC replacement for `Crypto.sign`, use `org.keyczar.Signer`.

If you need a symmetric encryption replacement for `Crypto.encryptAES`, then use `org.keyczar.Crypter`.

### JCA

Both Kalium and Keyczar use different cryptographic primitives than Crypto.  For users who intend to migrate from Crypto functionality without changing the underlying algorithms, the best option is probably to extract the code from the Crypto library to a user level class.

### Further Reading

There are some papers available on cryptographic design that go over some of the issues addressed by crypto APIs and the complexities involved:

* [The Long Journey from Papers to Software: Crypto APIs](https://crypto.junod.info/IACR15_crypto_school_talk.pdf)
* [What’s Wrong with Crypto API Design](http://spar.isi.jhu.edu/~mgreen/CryptoAPIs.pdf)
* [Real World Crypto 2015: Error-prone cryptographic designs (djb)](http://bristolcrypto.blogspot.com/2015/01/real-world-crypto-2015-error-prone.html) and [slides](http://cr.yp.to/talks/2015.01.07/slides-djb-20150107-a4.pdf)
