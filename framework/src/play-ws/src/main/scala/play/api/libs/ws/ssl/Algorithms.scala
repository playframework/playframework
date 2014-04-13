/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import javax.crypto.SecretKey
import java.security.interfaces._
import javax.crypto.interfaces.DHKey
import scala.util.parsing.combinator.RegexParsers
import java.security.{ KeyFactory, Key }
import scala.Some

object Algorithms {

  // The jdk.tls.disabledAlgorithms property applies to TLS handshaking,
  // and the jdk.certpath.disabledAlgorithms property applies to certification path processing.

  def disabledSignatureAlgorithms: String = "MD2, MD4, MD5"

  def disabledKeyAlgorithms: String = "RSA keySize < 1024, DSA keySize < 1024, EC keySize < 160"

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

  def apply(input: String): List[AlgorithmConstraint] = parseAll(line, input) match {
    case Success(result, _) =>
      result
    case NoSuccess(message, _) =>
      throw new IllegalArgumentException(s"Cannot parse string $input: $message")
  }

  def line: Parser[List[AlgorithmConstraint]] = repsep(expression, ",")

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

