/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.crypto

import java.nio.charset.StandardCharsets
import java.security.{ MessageDigest, SecureRandom }
import java.time.Clock
import javax.inject.{ Inject, Provider, Singleton }

import org.apache.commons.codec.binary.Hex

/**
 * Cryptographic utilities for generating and validating CSRF tokens.
 *
 * This trait should not be used as a general purpose encryption utility.
 */
trait CSRFTokenSigner {

  /**
   * Sign a token.  This produces a new token, that has this token signed with a nonce.
   *
   * This primarily exists to defeat the BREACH vulnerability, as it allows the token
   * to effectively be random per request, without actually changing the value.
   *
   * @param token The token to sign
   * @return The signed token
   */
  def signToken(token: String): String

  /**
   * Generates a cryptographically secure token.
   */
  def generateToken: String

  /**
   * Generates a signed token.
   */
  def generateSignedToken: String

  /**
   * Extract a signed token that was signed by `signToken(String)`.
   *
   * @param token The signed token to extract.
   * @return The verified raw token, or None if the token isn't valid.
   */
  def extractSignedToken(token: String): Option[String]

  /**
   * Compare two signed tokens
   */
  def compareSignedTokens(tokenA: String, tokenB: String): Boolean

  /**
   * Constant time equals method.
   *
   * Given a length that both Strings are equal to, this method will always
   * run in constant time.  This prevents timing attacks.
   *
   * @deprecated Please use `java.security.MessageDigest.isEqual(a.getBytes("utf-8"), b.getBytes("utf-8"))` over this method.
   */
  @deprecated("Please use java.security.MessageDigest.isEqual(a.getBytes(\"utf-8\"), b.getBytes(\"utf-8\")) over this method.", "2.6.0")
  def constantTimeEquals(a: String, b: String): Boolean
}

/**
 * This class is used for generating random tokens for CSRF.
 */
class DefaultCSRFTokenSigner @Inject() (signer: CookieSigner, clock: Clock) extends CSRFTokenSigner {

  // If you're running on an older version of Windows, you may be using
  // SHA1PRNG.  So immediately calling nextBytes with a seed length
  // of 440 bits (NIST SP800-90A) will do a more than decent
  // self-seeding for a SHA-1 based PRNG.
  private val random = new SecureRandom()
  random.nextBytes(new Array[Byte](55))

  /**
   * Sign a token.  This produces a new token, that has this token signed with a nonce.
   *
   * This primarily exists to defeat the BREACH vulnerability, as it allows the token to effectively be random per
   * request, without actually changing the value.
   *
   * @param token The token to sign
   * @return The signed token
   */
  def signToken(token: String): String = {
    val nonce = clock.millis()
    val joined = nonce + "-" + token
    signer.sign(joined) + "-" + joined
  }

  /**
   * Extract a signed token that was signed by [[CSRFTokenSigner.signToken]].
   *
   * @param token The signed token to extract.
   * @return The verified raw token, or None if the token isn't valid.
   */
  def extractSignedToken(token: String): Option[String] = {
    token.split("-", 3) match {
      case Array(signature, nonce, raw) if isEqual(signature, signer.sign(nonce + "-" + raw)) => Some(raw)
      case _ => None
    }
  }

  /**
   * Generate a cryptographically secure token
   */
  def generateToken: String = {
    val bytes = new Array[Byte](12)
    random.nextBytes(bytes)
    new String(Hex.encodeHex(bytes))
  }

  /**
   * Generate a signed token
   */
  def generateSignedToken: String = signToken(generateToken)

  /**
   * Compare two signed tokens
   */
  def compareSignedTokens(tokenA: String, tokenB: String): Boolean = {
    (for {
      rawA <- extractSignedToken(tokenA)
      rawB <- extractSignedToken(tokenB)
    } yield isEqual(rawA, rawB)).getOrElse(false)
  }

  override def constantTimeEquals(a: String, b: String): Boolean = isEqual(a, b)

  private def isEqual(a: String, b: String): Boolean = {
    MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8))
  }
}

@deprecated("CSRFTokenSigner's singleton object can be replaced by MessageDigest.isEqual", "2.6.0")
object CSRFTokenSigner {

  /**
   * @deprecated Please use [[java.security.MessageDigest.isEqual]] over this method.
   */
  @deprecated("Consider java.security.MessageDigest.isEqual", "2.6.0")
  def constantTimeEquals(a: String, b: String): Boolean = {
    MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8))
  }
}

@Singleton
class CSRFTokenSignerProvider @Inject() (signer: CookieSigner) extends Provider[CSRFTokenSigner] {
  lazy val get: CSRFTokenSigner = new DefaultCSRFTokenSigner(signer, Clock.systemUTC())
}
