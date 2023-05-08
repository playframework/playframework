/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.crypto;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Cryptographic utilities for generating and validating CSRF tokens.
 *
 * <p>This trait should not be used as a general purpose encryption utility.
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

  @Override
  public play.api.libs.crypto.CSRFTokenSigner asScala() {
    return csrfTokenSigner;
  }
}
