/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.crypto;

/**
 * Authenticates a cookie by returning a message authentication code (MAC).
 * <p>
 * This interface should not be used as a general purpose MAC utility.
 */
public interface CookieSigner {

    /**
     * Signs the given String using the application's secret key.
     * <br>
     * By default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     *
     * @param message The message to sign.
     * @return A hexadecimal encoded signature.
     */
    String sign(String message);

    /**
     * Signs the given String using the given key.
     * <br>
     * By default this uses the platform default JSSE provider.  This can be overridden by defining
     * <code>application.crypto.provider</code> in <code>application.conf</code>.
     *
     * @param message The message to sign.
     * @param key     The private key to sign with.
     * @return A hexadecimal encoded signature.
     */
    String sign(String message, byte[] key);

    /**
     * @return The Scala version for this cookie signer.
     */
    play.api.libs.crypto.CookieSigner asScala();
}
