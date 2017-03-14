package play.libs.crypto;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Delegates to a Scala CookieSigner.
 */
@Singleton
public class DelegateCookieSigner implements CookieSigner {

    private final play.api.libs.crypto.CookieSigner signer;

    @Inject
    public DelegateCookieSigner(play.api.libs.crypto.CookieSigner signer) {
        this.signer = signer;
    }

    /**
     * Signs the cookie with the default key.
     *
     * @param message The message to sign.
     * @return A hexadecimal encoded signature.
     */
    public String sign(String message) {
        return signer.sign(message);
    }

    /**
     * Signs the cookie with the given key.
     *
     * @param message The message to sign.
     * @param key     The private key to sign with.
     * @return A hexadecimal encoded signature.
     */
    public String sign(String message, byte[] key) {
        return signer.sign(message, key);
    }

    public play.api.libs.crypto.CookieSigner asScala() {
        return this.signer;
    }

}
