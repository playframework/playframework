/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import org.specs2.mutable._
import scala.util.parsing.input.CharSequenceReader
import org.specs2.matcher.{ ExpectedParsedResult, ParserMatchers }

object AlgorithmConstraintsParserSpec extends Specification with ParserMatchers {

  val parsers = AlgorithmConstraintsParser

  import AlgorithmConstraintsParser._

  "AlgorithmConstraintParser" should {

    "succeed to recognize 'RSA'" in {
      expression must succeedOn("RSA").withResult(AlgorithmConstraint("RSA"))
    }

    "succeed to recognize 'RSA keySize < 1024'" in {
      expression must succeedOn("RSA keySize < 1024").withResult(AlgorithmConstraint("RSA", Some(LessThan(1024))))
    }

    "succeed to recognize 'RSA keySize <= 1024'" in {
      expression must succeedOn("RSA keySize <= 1024").withResult(AlgorithmConstraint("RSA", Some(LessThanOrEqual(1024))))
    }

    "succeed to recognize 'EC keySize > 1024'" in {
      expression must succeedOn("EC keySize > 1024").withResult(AlgorithmConstraint("EC", Some(MoreThan(1024))))
    }

    "succeed to recognize 'EC keySize >= 1024'" in {
      expression must succeedOn("EC keySize >= 1024").withResult(AlgorithmConstraint("EC", Some(MoreThanOrEqual(1024))))
    }

    "succeed to recognize 'EC keySize == 1024'" in {
      expression must succeedOn("EC keySize == 1024").withResult(AlgorithmConstraint("EC", Some(Equal(1024))))
    }

    "succeed to recognize 'EC keySize != 1024'" in {
      expression must succeedOn("EC keySize != 1024").withResult(AlgorithmConstraint("EC", Some(NotEqual(1024))))
    }

    "fail to recognize 'EC > 1024'" in {
      expression must failOn("EC > 1024")
    }

    "fail to recognize 'EC keySize'" in {
      expression must failOn("EC keySize")
    }

    "fail to recognize ''" in {
      expression must failOn("")
    }

  }

}
