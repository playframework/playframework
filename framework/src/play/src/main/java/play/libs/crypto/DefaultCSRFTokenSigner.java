/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.crypto;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Cryptographic utilities for generating and validating CSRF tokens.
 * <p>
 * This trait should not be used as a general purpose encryption utility.
 */
@Singleton
public class DefaultCSRFTokenSigner implements CSRFTokenSigner {

    private final play.api.libs.crypto.CSRFTokenSigner csrfTokenSigner;

    @Inject
    public DefaultCSRFTokenSigner(play.api.libs.crypto.CSRFTokenSigner csrfTokenSigner) {
        this.csrfTokenSigner = csrfTokenSigner;
    }

    public String signToken(String token) {
        return csrfTokenSigner.signToken(token);
    }

    public String extractSignedToken(String token) {
        scala.Option<String> extracted = csrfTokenSigner.extractSignedToken(token);
        if (extracted.isDefined()) {
            return extracted.get();
        } else {
            return null;
        }
    }

    public String generateToken() {
        return csrfTokenSigner.generateToken();
    }

    public String generateSignedToken() {
        return csrfTokenSigner.generateSignedToken();
    }

    public boolean compareSignedTokens(String tokenA, String tokenB) {
        return csrfTokenSigner.compareSignedTokens(tokenA, tokenB);
    }

    @Deprecated
    public boolean constantTimeEquals(String a, String b) {
        return csrfTokenSigner.constantTimeEquals(a, b);
    }

    @Override
    public play.api.libs.crypto.CSRFTokenSigner asScala() {
        return csrfTokenSigner;
    }

}
