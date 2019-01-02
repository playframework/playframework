/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.crypto;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class delegates to the Scala CookieSigner.
 */
@Singleton
public class DefaultCookieSigner implements CookieSigner {

    private final play.api.libs.crypto.CookieSigner signer;

    @Inject
    public DefaultCookieSigner(play.api.libs.crypto.CookieSigner signer) {
        this.signer = signer;
    }

    /**
     * Signs the given String using the application's secret key.
     *
     * @param message The message to sign.
     * @return A hexadecimal encoded signature.
     */
    @Override
    public String sign(String message) {
        return signer.sign(message);
    }

    /**
     * Signs the given String using the given key.
     * <br>
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
