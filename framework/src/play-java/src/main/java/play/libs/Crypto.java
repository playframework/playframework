package play.libs;

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
public class Crypto {

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
    public static String sign(String message, byte[] key) {
        return play.api.libs.Crypto.sign(message, key);
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
    public static String sign(String message) {
        return play.api.libs.Crypto.sign(message);
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
    public static String signToken(String token) {
        return play.api.libs.Crypto.signToken(token);
    }

    /**
     * Extract a signed token that was signed by {@link #signToken(String)}.
     *
     * @param token The signed token to extract.
     * @return The verified raw token, or null if the token isn't valid.
     */
    public static String extractSignedToken(String token) {
        scala.Option<String> extracted = play.api.libs.Crypto.extractSignedToken(token);
        if (extracted.isDefined()) {
            return extracted.get();
        } else {
            return null;
        }
    }

    /**
     * Generate a cryptographically secure token
     */
    public static String generateToken() {
        return play.api.libs.Crypto.generateToken();
    }

    /**
     * Generate a signed token
     */
    public static String generateSignedToken() {
        return play.api.libs.Crypto.generateSignedToken();
    }

    /**
     * Compare two signed tokens
     */
    public static boolean compareSignedTokens(String tokenA, String tokenB) {
        return play.api.libs.Crypto.compareSignedTokens(tokenA, tokenB);
    }

    /**
     * Constant time equals method.
     *
     * Given a length that both Strings are equal to, this method will always run in constant time.  This prevents
     * timing attacks.
     */
    public static boolean constantTimeEquals(String a, String b) {
        return play.api.libs.Crypto.constantTimeEquals(a, b);
    }

    /**
     * Encrypt a String with the AES encryption standard using the application's secret key.
     * <br>
     * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     * <br>
     * The transformation algorithm used is the provider specific implementation of the <code>AES</code> name.  On
     * Oracles JDK, this is <code>AES/ECB/PKCS5Padding</code>.  This algorithm is suitable for small amounts of data,
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
    public static String encryptAES(String value) {
        return play.api.libs.Crypto.encryptAES(value);
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
     * Oracles JDK, this is <code>AES/ECB/PKCS5Padding</code>.  This algorithm is suitable for small amounts of data,
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
    public static String encryptAES(String value, String privateKey) {
        return play.api.libs.Crypto.encryptAES(value, privateKey);
    }

    /**
     * Decrypt a String with the AES encryption standard using the application's secret key.
     * <br>
     * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     * <br>
     * The transformation used is by default <code>AES/ECB/PKCS5Padding</code>.  It can be configured by defining
     * <code>application.crypto.aes.transformation</code> in <code>application.conf</code>.  Although any cipher
     * transformation algorithm can be selected here, the secret key spec used is always AES, so only AES transformation
     * algorithms will work.
     *
     * @param value An hexadecimal encrypted string.
     * @return The decrypted String.
     */
    public static String decryptAES(String value) {
        return play.api.libs.Crypto.decryptAES(value);
    }

    /**
     * Decrypt a String with the AES encryption standard.
     * <br>
     * The private key must have a length of 16 bytes.
     * <br>
     * The provider used is by default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     * <br>
     * The transformation used is by default <code>AES/ECB/PKCS5Padding</code>.  It can be configured by defining
     * <code>application.crypto.aes.transformation</code> in <code>application.conf</code>.  Although any cipher
     * transformation algorithm can be selected here, the secret key spec used is always AES, so only AES transformation
     * algorithms will work.
     *
     * @param value      An hexadecimal encrypted string.
     * @param privateKey The key used to encrypt.
     * @return The decrypted String.
     */
    public static String decryptAES(String value, String privateKey) {
        return play.api.libs.Crypto.decryptAES(value, privateKey);
    }

}
