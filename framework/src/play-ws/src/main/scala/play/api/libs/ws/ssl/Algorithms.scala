/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import javax.crypto.SecretKey
import java.security.interfaces._
import javax.crypto.interfaces.DHKey
import scala.util.parsing.combinator.RegexParsers
import java.security.{ KeyFactory, Key }
import scala.Some

/**
 * This singleton object provides the code needed to check for minimum standards of an X.509 certificate.  Over 95% of trusted leaf certificates and 95% of trusted signing certificates use <a href="http://csrc.nist.gov/publications/nistpubs/800-131A/sp800-131A.pdf">NIST recommended key sizes</a>.  Play supports Java 1.6, which does not have built in <a href="http://sim.ivi.co/2013/11/harness-ssl-and-jsse-key-size-control.html">certificate strength checking</a>, so we roll our own here.
 *
 * The default settings here are based off <a href="">NIST SP 800-57</a>, using <a href="https://wiki.mozilla.org/CA:MD5and1024">Dates for Phasing out MD5-based signatures and 1024-bit moduli</a> as a practical guide.
 *
 * Note that the key sizes are checked on root CA certificates in the trust store.  As the Mozilla document says:
 *
 * <blockquote>The other concern that needs to be addressed is that of RSA1024 being too small a modulus to be robust against faster computers. Unlike a signature algorithm, where only intermediate and end-entity certificates are impacted, fast math means we have to disable or remove all instances of 1024-bit moduli, including the root certificates.</blockquote>
 *
 * Relevant key sizes:
 *
 * <blockquote>
 * According to NIST SP 800-57 the recommended algorithms and minimum key sizes are as follows:
 *
 * Through 2010 (minimum of 80 bits of strength)
 * FFC (e.g., DSA, D-H) Minimum: L=1024; N=160
 * IFC (e.g., RSA) Minimum: k=1024
 * ECC (e.g. ECDSA) Minimum: f=160
 * Through 2030 (minimum of 112 bits of strength)
 * FFC (e.g., DSA, D-H) Minimum: L=2048; N=224
 * IFC (e.g., RSA) Minimum: k=2048
 * ECC (e.g. ECDSA) Minimum: f=224
 * Beyond 2030 (minimum of 128 bits of strength)
 * FFC (e.g., DSA, D-H) Minimum: L=3072; N=256
 * IFC (e.g., RSA) Minimum: k=3072
 * ECC (e.g. ECDSA) Minimum: f=256
 * </blockquote>
 *
 * Relevant signature algorithms:
 *
 * The known weak signature algorithms are "MD2, MD4, MD5".
 *
 * SHA-1 is considered too weak for new certificates, but is <a href="http://csrc.nist.gov/groups/ST/hash/policy.html">still allowed</a> for verifying old certificates in the chain.  The <a href="https://blogs.oracle.com/xuelei/entry/tls_and_nist_s_policy">TLS and NIST'S Policy on Hash Functions</a> blog post by one of the JSSE authors has more details, in particular the "Put it into practice" section.
 */
object Algorithms {

  /**
   * Disabled signature algorithms are applied to signed certificates in a certificate chain, not including CA certs.
   *
   * @return "MD2, MD4, MD5"
   */
  def disabledSignatureAlgorithms: String = "MD2, MD4, MD5"

  /**
   * Disabled key algorithms are applied to all certificates, including the root CAs.
   *
   * @return "RSA keySize < 2048, DSA keySize < 2048, EC keySize < 224"
   */
  def disabledKeyAlgorithms: String = "RSA keySize < 2048, DSA keySize < 2048, EC keySize < 224"

  /**
   * Returns the keySize of the given key, or None if no key exists.
   */
  def keySize(key: java.security.Key): Option[Int] = {
    key match {
      case sk: SecretKey =>
        if ((sk.getFormat == "RAW") && sk.getEncoded != null) {
          Some(sk.getEncoded.length * 8)
        } else {
          None
        }
      case pubk: RSAKey =>
        Some(pubk.getModulus.bitLength)
      case pubk: ECKey =>
        Some(pubk.getParams.getOrder.bitLength())
      case pubk: DSAKey =>
        Some(pubk.getParams.getP.bitLength)
      case pubk: DHKey =>
        Some(pubk.getParams.getP.bitLength)
      case pubk: Key =>
        val translatedKey = translateKey(pubk)
        keySize(translatedKey)
      case unknownKey =>
        try {
          val lengthMethod = unknownKey.getClass.getMethod("length")
          val l = lengthMethod.invoke(unknownKey).asInstanceOf[Integer]
          if (l >= 0) Some(l) else None
        } catch {
          case _: Throwable =>
            throw new IllegalStateException(s"unknown key ${key.getClass.getName}")
        }
      // None
    }
  }

  def getKeyAlgorithmName(pubk: Key): String = {
    val name = pubk.getAlgorithm
    if (name == "DH") "DiffieHellman" else name
  }

  def translateKey(pubk: Key): Key = {
    val keyAlgName = getKeyAlgorithmName(pubk)
    foldVersion(
      run16 = {
        keyAlgName match {
          case "EC" =>
            // If we are on 1.6, then we can't use the EC factory and have to pull it directly.
            translateECKey(pubk)
          case _ =>
            val keyFactory = KeyFactory.getInstance(keyAlgName)
            keyFactory.translateKey(pubk)
        }
      },
      runHigher = {
        val keyFactory = KeyFactory.getInstance(keyAlgName)
        keyFactory.translateKey(pubk)
      }
    )
  }

  def translateECKey(pubk: Key): Key = {
    val keyFactory = Thread.currentThread().getContextClassLoader.loadClass("sun.security.ec.ECKeyFactory")
    val method = keyFactory.getMethod("toECKey", classOf[java.security.Key])
    method.invoke(null, pubk) match {
      case e: ECPublicKey =>
        e
      case e: ECPrivateKey =>
        e
    }
  }

  /**
   * Decompose the standard algorithm name into sub-elements.
   * <p/>
   * For example, we need to decompose "SHA1WithRSA" into "SHA1" and "RSA"
   * so that we can check the "SHA1" and "RSA" algorithm constraints
   * separately.
   * <p/>
   * Please override the method if need to support more name pattern.
   */
  def decomposes(algorithm: String): Set[String] = {
    if (algorithm == null || algorithm.length == 0) {
      throw new IllegalArgumentException("Null or blank algorithm found!")
    }

    val withAndPattern = new scala.util.matching.Regex("(?i)with|and")
    val tokens: Array[String] = "/".r.split(algorithm)
    val elements = (for {
      t <- tokens
      name <- withAndPattern.split(t)
    } yield {
      name
    }).toSet

    if (elements.contains("SHA1") && !elements.contains("SHA-1")) {
      elements + "SHA-1"
    } else if (elements.contains("SHA-1") && !elements.contains("SHA1")) {
      elements + "SHA1"
    } else {
      elements
    }
  }

}

sealed abstract class ExpressionSymbol {
  def matches(actualKeySize: Int): Boolean
}

case class LessThan(x: Int) extends ExpressionSymbol {
  def matches(actualKeySize: Int): Boolean = actualKeySize < x

  override def toString = " keySize < " + x
}

case class LessThanOrEqual(x: Int) extends ExpressionSymbol {
  def matches(actualKeySize: Int): Boolean = actualKeySize <= x

  override def toString = " keySize <= " + x
}

case class NotEqual(x: Int) extends ExpressionSymbol {
  def matches(actualKeySize: Int): Boolean = actualKeySize != x

  override def toString = " keySize != " + x
}

case class Equal(x: Int) extends ExpressionSymbol {
  def matches(actualKeySize: Int): Boolean = actualKeySize == x

  override def toString = " keySize ==" + x
}

case class MoreThan(x: Int) extends ExpressionSymbol {
  def matches(actualKeySize: Int): Boolean = actualKeySize > x

  override def toString = " keySize > " + x
}

case class MoreThanOrEqual(x: Int) extends ExpressionSymbol {
  def matches(actualKeySize: Int): Boolean = actualKeySize >= x

  override def toString = " keySize >= " + x
}

case class AlgorithmConstraint(algorithm: String, constraint: Option[ExpressionSymbol] = None) {

  /**
   * Returns true only if the algorithm matches.  Useful for signature algorithms where we don't care about key size.
   */
  def matches(algorithm: String): Boolean = {
    this.algorithm.equalsIgnoreCase(algorithm)
  }

  /**
   * Returns true if the algorithm name matches, and if there's a keySize constraint, will match on that as well.
   */
  def matches(algorithm: String, keySize: Int): Boolean = {
    if (!matches(algorithm)) {
      return false
    }

    constraint match {
      case Some(expression) =>
        expression.matches(keySize)

      case None =>
        true
    }
  }

  override def toString = {
    algorithm + constraint.getOrElse("")
  }
}

/**
 * Parser based on the jdk.certpath.disabledAlgorithm BNF.
 *
 * @see http://sim.ivi.co/2011/07/java-se-7-release-security-enhancements.html
 */
object AlgorithmConstraintsParser extends RegexParsers {

  import scala.language.postfixOps

  def apply(input: String): AlgorithmConstraint = parseAll(expression, input) match {
    case Success(result, _) =>
      result
    case NoSuccess(message, _) =>
      throw new IllegalArgumentException(s"Cannot parse string $input: $message")
  }

  def expression: Parser[AlgorithmConstraint] = algorithm ~ (keySizeConstraint ?) ^^ {
    case algorithm ~ Some(constraint) =>
      AlgorithmConstraint(algorithm, Some(constraint))

    case algorithm ~ None =>
      AlgorithmConstraint(algorithm, None)
  }

  def keySizeConstraint: Parser[ExpressionSymbol] = "keySize" ~> operator ~ decimalInteger ^^ {
    case "<=" ~ decimal =>
      LessThanOrEqual(decimal)

    case "<" ~ decimal =>
      LessThan(decimal)

    case "==" ~ decimal =>
      Equal(decimal)

    case "!=" ~ decimal =>
      NotEqual(decimal)

    case ">=" ~ decimal =>
      MoreThanOrEqual(decimal)

    case ">" ~ decimal =>
      MoreThan(decimal)
  }

  def operator: Parser[String] = "<=" | "<" | "==" | "!=" | ">=" | ">"

  def decimalInteger: Parser[Int] = """\d+""".r ^^ {
    f => f.toInt
  }

  def algorithm: Parser[String] = """\w+""".r ^^ {
    f => f.toString
  }

}

