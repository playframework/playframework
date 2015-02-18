/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Cryptographic utilities.
 * <br>
 * These utilities are intended as a convenience, however it is important to read each methods documentation and
 * understand the concepts behind encryption to use this class properly.  Safe encryption is hard, and there is no
 * substitute for an adequate understanding of cryptography.  These methods will not be suitable for all encryption
 * needs.
 *
 * For more information about cryptography, we recommend reading the OWASP Cryptographic Storage Cheatsheet:
 *
 * https://www.owasp.org/index.php/Cryptographic_Storage_Cheat_Sheet
 */
@Singleton
public class Crypto {

    private final play.api.libs.Crypto crypto;

    @Inject
    public Crypto(play.api.libs.Crypto crypto) {
        this.crypto = crypto;
    }

    /**
     * Signs the given String with HMAC-SHA1 using the given key.
     * <br>
     * By default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     *
     * @param message The message to sign.
     * @param key     The private key to sign with.
     * @return A hexadecimal encoded signature.
     */
    public String sign(String message, byte[] key) {
        return crypto.sign(message, key);
    }

    /**
     * Signs the given String with HMAC-SHA1 using the application's secret key.
     * <br>
     * By default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     *
     * @param message The message to sign.
     * @return A hexadecimal encoded signature.
     */
    public String sign(String message) {
        return crypto.sign(message);
    }

    /**
     * Sign a token.  This produces a new token, that has this token signed with a nonce.
     *
     * This primarily exists to defeat the BREACH vulnerability, as it allows the token to effectively be random per
     * request, without actually changing the value.
     *
     * @param token The token to sign
     * @return The signed token
     */
    public String signToken(String token) {
        return crypto.signToken(token);
    }

    /**
     * Extract a signed token that was signed by {@link #signToken(String)}.
     *
     * @param token The signed token to extract.
     * @return The verified raw token, or null if the token isn't valid.
     */
    public String extractSignedToken(String token) {
        scala.Option<String> extracted = crypto.extractSignedToken(token);
        if (extracted.isDefined()) {
            return extracted.get();
        } else {
            return null;
        }
    }

    /**
     * Generate a cryptographically secure token
     */
    public String generateToken() {
        return crypto.generateToken();
    }

    /**
     * Generate a signed token
     */
    public String generateSignedToken() {
        return crypto.generateSignedToken();
    }

    /**
     * Compare two signed tokens
     */
    public boolean compareSignedTokens(String tokenA, String tokenB) {
        return crypto.compareSignedTokens(tokenA, tokenB);
    }

    /**
     * Constant time equals method.
     *
     * Given a length that both Strings are equal to, this method will always run in constant time.  This prevents
     * timing attacks.
     */
    public boolean constantTimeEquals(String a, String b) {
        return crypto.constantTimeEquals(a, b);
    }

    /**
     * Encrypt a String with the AES encryption standard using the application's secret key.
     * <br>
     * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     * <br>
     * The transformation algorithm used is the provider specific implementation of the <code>AES</code> name.  On
     * Oracles JDK, this is <code>AES/CTR/NoPadding</code>.  This algorithm is suitable for small amounts of data,
     * typically less than 32 bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger
     * blocks of data, this algorithm may expose patterns and be vulnerable to repeat attacks.
     * <br>
     * The transformation algorithm can be configured by defining <code>application.crypto.aes.transformation</code> in
     * <code>application.conf</code>.  Although any cipher transformation algorithm can be selected here, the secret key
     * spec used is always AES, so only AES transformation algorithms will work.
     *
     * @param value The String to encrypt.
     * @return An hexadecimal encrypted string.
     */
    public String encryptAES(String value) {
        return crypto.encryptAES(value);
    }

    /**
     * Encrypt a String with the AES encryption standard and the supplied private key.
     * <br>
     * The private key must have a length of 16 bytes.
     * <br>
     * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     * <br>
     * The transformation algorithm used is the provider specific implementation of the <code>AES</code> name.  On
     * Oracles JDK, this is <code>AES/CTR/NoPadding</code>.  This algorithm is suitable for small amounts of data,
     * typically less than 32bytes, hence is useful for encrypting credit card numbers, passwords etc.  For larger
     * blocks of data, this algorithm may expose patterns and be vulnerable to repeat attacks.
     * <br>
     * The transformation algorithm can be configured by defining <code>application.crypto.aes.transformation</code> in
     * <code>application.conf</code>.  Although any cipher transformation algorithm can be selected here, the secret key
     * spec used is always AES, so only AES transformation algorithms will work.
     *
     * @param value      The String to encrypt.
     * @param privateKey The key used to encrypt.
     * @return An hexadecimal encrypted string.
     */
    public String encryptAES(String value, String privateKey) {
        return crypto.encryptAES(value, privateKey);
    }

    /**
     * Decrypt a String with the AES encryption standard using the application's secret key.
     * <br>
     * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     * <br>
     * The transformation used is by default <code>AES/CTR/NoPadding</code>.  It can be configured by defining
     * <code>application.crypto.aes.transformation</code> in <code>application.conf</code>.  Although any cipher
     * transformation algorithm can be selected here, the secret key spec used is always AES, so only AES transformation
     * algorithms will work.
     *
     * @param value An hexadecimal encrypted string.
     * @return The decrypted String.
     */
    public String decryptAES(String value) {
        return crypto.decryptAES(value);
    }

    /**
     * Decrypt a String with the AES encryption standard.
     * <br>
     * The private key must have a length of 16 bytes.
     * <br>
     * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     * <br>
     * The transformation used is by default <code>AES/CTR/NoPadding</code>.  It can be configured by defining
     * <code>application.crypto.aes.transformation</code> in <code>application.conf</code>.  Although any cipher
     * transformation algorithm can be selected here, the secret key spec used is always AES, so only AES transformation
     * algorithms will work.
     *
     * @param value      An hexadecimal encrypted string.
     * @param privateKey The key used to encrypt.
     * @return The decrypted String.
     */
    public String decryptAES(String value, String privateKey) {
        return crypto.decryptAES(value, privateKey);
    }

}
