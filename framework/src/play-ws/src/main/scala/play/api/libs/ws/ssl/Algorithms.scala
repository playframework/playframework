/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import javax.crypto.SecretKey
import java.security.interfaces.{ DSAKey, ECKey, RSAKey }
import javax.crypto.interfaces.DHKey
import scala.util.parsing.combinator.RegexParsers

object Algorithms {

  //  # The syntax of the disabled algorithm string is described as this Java
  //  # BNF-style:
  //  #   DisabledAlgorithms:
  //  #       " DisabledAlgorithm { , DisabledAlgorithm } "
  //  #
  //  #   DisabledAlgorithm:
  //  #       AlgorithmName [Constraint]
  //  #
  //  #   AlgorithmName:
  //  #       (see below)
  //  #
  //  #   Constraint:
  //  #       KeySizeConstraint
  //  #
  //  #   KeySizeConstraint:
  //  #       "keySize" Operator DecimalInteger
  //  #
  //  #   Operator:
  //  #       <= | < | == | != | >= | >
  //  #
  //  #   DecimalInteger:
  //  #       DecimalDigits
  //  #
  //  #   DecimalDigits:
  //  #       DecimalDigit {DecimalDigit}
  //  #
  //  #   DecimalDigit: one of
  //  #       1 2 3 4 5 6 7 8 9 0

  // See http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/sun/security/ssl/SSLAlgorithmConstraints.java
  // http://sim.ivi.co/2013/11/harness-ssl-and-jsse-key-size-control.html
  // http://marc.info/?l=openjdk-security-dev&m=138932097003284&w=2
  // http://openjdk.5641.n7.nabble.com/Code-Review-Request-7109274-Consider-disabling-support-for-X-509-certificates-with-RSA-keys-less-thas-td107890.html
  // http://grokbase.com/t/gg/play-framework/14199wzbgp/2-2-scala-disabling-diffie-hellman-cipher-suites
  //
  // The jdk.tls.disabledAlgorithms property applies to TLS handshaking,
  // and the jdk.certpath.disabledAlgorithms property applies to certification path processing.

  // The closest analogue is jdk.certpath.disabledAlgorithms
  def disabledSignatureAlgorithms: String = "MD2, MD4, MD5"

  def disabledKeyAlgorithms: String = "RSA keySize < 1024, DSA keySize < 1024, EC keySize < 160"

  /**
   * Returns the keySize of the given key.
   */
  def keySize(key: java.security.Key): Int = {
    key match {
      case sk: SecretKey =>
        if ((sk.getFormat == "RAW") && sk.getEncoded != null) {
          sk.getEncoded.length * 8
        } else {
          -1
        }
      case pubk: RSAKey =>
        pubk.getModulus.bitLength
      case pubk: ECKey =>
        pubk.getParams.getOrder.bitLength
      case pubk: DSAKey =>
        pubk.getParams.getP.bitLength
      case pubk: DHKey =>
        pubk.getParams.getP.bitLength
      case _ =>
        -1
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
      return Set()
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

