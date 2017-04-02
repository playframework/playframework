package play.libs.crypto;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Authenticates a cookie by creating a  message authentication code using HMAC-SHA1.
 */
@Singleton
public class HMACSHA1CookieSigner implements CookieSigner {

    private final play.api.libs.crypto.CookieSigner signer;

    @Inject
    public HMACSHA1CookieSigner(play.api.libs.crypto.CookieSigner signer) {
        this.signer = signer;
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
    @Override
    public String sign(String message) {
        return signer.sign(message);
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
    @Override
    public String sign(String message, byte[] key) {
        return signer.sign(message, key);
    }

    @Override
    public play.api.libs.crypto.CookieSigner asScala() {
        return this.signer;
    }

}
