/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.crypto;

/**
 * Cryptographic utilities for generating and validating CSRF tokens.
 * <p>
 * This trait should not be used as a general purpose encryption utility.
 */
public interface CSRFTokenSigner {

    /**
     * Generates a cryptographically secure token.
     */
    String generateToken();

    /**
     * Generates a signed token by calling generateToken / signToken.
     */
    String generateSignedToken();

    /**
     * Sign a token.  This produces a new token, that has this token signed with a nonce.
     * <p>
     * This primarily exists to defeat the BREACH vulnerability, as it allows the token
     * to effectively be random per request, without actually changing the value.
     *
     * @param token The token to sign
     * @return The signed token
     */
    String signToken(String token);

    /**
     * Extract a signed token that was signed by {@link #signToken(String)}.
     *
     * @param token The signed token to extract.
     * @return The verified raw token, or null if the token isn't valid.
     */
    String extractSignedToken(String token);

    /**
     * Compare two signed tokens.
     */
    boolean compareSignedTokens(String tokenA, String tokenB);

    /**
     * Constant time equals method.
     * <p>
     * Given a length that both Strings are equal to, this method will always run in constant time.  This prevents
     * timing attacks.
     *
     * @deprecated since 2.6.0.  Use java.security.MessageDigest.isEqual over this method.
     */
    @Deprecated
    boolean constantTimeEquals(String a, String b);

    /**
     * Utility method needed for CSRFCheck.
     */
    play.api.libs.crypto.CSRFTokenSigner asScala();
}
