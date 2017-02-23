/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import javax.inject.Inject

import play.api._
import play.api.libs.crypto._

@javax.inject.Singleton
@deprecated("This class is deprecated and will be removed in future versions", "2.5.0")
class Crypto @Inject() (signer: CookieSigner, tokenSigner: CSRFTokenSigner, aesCrypter: AESCrypter) extends CookieSigner with CSRFTokenSigner with AESCrypter {

  override def signToken(token: String): String = tokenSigner.signToken(token)

  override def extractSignedToken(token: String): Option[String] = tokenSigner.extractSignedToken(token)

  override def compareSignedTokens(tokenA: String, tokenB: String): Boolean = tokenSigner.compareSignedTokens(tokenA, tokenB)

  override def generateToken: String = tokenSigner.generateToken

  override def constantTimeEquals(a: String, b: String): Boolean = tokenSigner.constantTimeEquals(a, b)

  override def generateSignedToken: String = tokenSigner.generateSignedToken

  override def decryptAES(value: String): String = aesCrypter.decryptAES(value)

  override def decryptAES(value: String, privateKey: String): String = aesCrypter.decryptAES(value, privateKey)

  override def encryptAES(value: String): String = aesCrypter.encryptAES(value)

  override def encryptAES(value: String, privateKey: String): String = aesCrypter.encryptAES(value, privateKey)

  override def sign(message: String, key: Array[Byte]): String = signer.sign(message)

  override def sign(message: String): String = signer.sign(message)
}

/**
 * This class is not suitable for use as a general cryptographic library.
 *
 * Please see <a href="https://www.playframework.com/documentation/2.5.x/CryptoMigration25">Crypto Migration Guide</a> for details, including how to migrate to another crypto system.
 *
 * @deprecated The singleton crypto object is deprecated as of 2.5.0
 */
@deprecated("This class is deprecated and will be removed in future versions", "2.5.0")
object Crypto {

  type CryptoException = play.api.libs.crypto.CryptoException

  private val cryptoCache: (Application) => Crypto = Application.instanceCache[Crypto]

  def crypto: Crypto = {
    Play.privateMaybeApplication.fold {
      sys.error("The global crypto instance requires a running application!")
    }(cryptoCache)
  }

  def sign(message: String, key: Array[Byte]): String = crypto.sign(message, key)

  def sign(message: String): String = crypto.sign(message)

  def signToken(token: String): String = crypto.signToken(token)

  def extractSignedToken(token: String): Option[String] = crypto.extractSignedToken(token)

  def generateToken: String = crypto.generateToken

  def generateSignedToken: String = crypto.generateSignedToken

  def compareSignedTokens(tokenA: String, tokenB: String): Boolean = crypto.compareSignedTokens(tokenA, tokenB)

  def constantTimeEquals(a: String, b: String): Boolean = crypto.constantTimeEquals(a, b)

  def encryptAES(value: String): String = crypto.encryptAES(value)

  def encryptAES(value: String, privateKey: String): String = crypto.encryptAES(value, privateKey)

  def decryptAES(value: String): String = crypto.decryptAES(value)

  def decryptAES(value: String, privateKey: String): String = crypto.decryptAES(value, privateKey)

}
