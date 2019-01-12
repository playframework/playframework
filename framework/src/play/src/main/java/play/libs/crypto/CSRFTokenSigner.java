/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
     * @return a newly generated token.
     */
    String generateToken();

    /**
     * Generates a signed token by calling generateToken / signToken.
     *
     * @return a newly generated token that has been signed.
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
     * @param tokenA the first token
     * @param tokenB another token
     * @return true if the tokens match and are signed, false otherwise.
     */
    boolean compareSignedTokens(String tokenA, String tokenB);

    /**
     * Utility method needed for CSRFCheck.  Should not need to be used or extended by user level code.
     *
     * @return the Scala API CSRFTokenSigner component.
     */
    play.api.libs.crypto.CSRFTokenSigner asScala();
}
