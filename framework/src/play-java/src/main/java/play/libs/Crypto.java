package play.libs;

public class Crypto {

    /**
     * Signs the given String with HMAC-SHA1 using the given key.
     */
    public static String sign(String message, byte[] key) {
        return play.api.libs.Crypto.sign(message, key);
    }

    /**
     * Signs the given String with HMAC-SHA1 using the application secret key.
     */
    public static String sign(String message) {
        return play.api.libs.Crypto.sign(message);
    }

    /**
     * Encrypt a String with the AES encryption standard using the application secret
     * @param value The String to encrypt
     * @return An hexadecimal encrypted string
     */
    public static String encryptAES(String value) {
        return play.api.libs.Crypto.encryptAES(value);
    }

    /**
     * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     * @param value The String to encrypt
     * @param privateKey The key used to encrypt
     * @return An hexadecimal encrypted string
     */
    public static String encryptAES(String value, String privateKey) {
        return play.api.libs.Crypto.encryptAES(value, privateKey);
    }

    /**
     * Decrypt a String with the AES encryption standard using the application secret
     * @param value An hexadecimal encrypted string
     * @return The decrypted String
     */
    public static String decryptAES(String value) {
        return play.api.libs.Crypto.decryptAES(value);
    }

    /**
     * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
     * @param value An hexadecimal encrypted string
     * @param privateKey The key used to encrypt
     * @return The decrypted String
     */
    public static String decryptAES(String value, String privateKey) {
        return play.api.libs.Crypto.decryptAES(value, privateKey);
    }

}
