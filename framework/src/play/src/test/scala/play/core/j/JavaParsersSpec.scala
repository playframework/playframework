package play.core.j

import java.math.{ BigDecimal, BigInteger }

import com.fasterxml.jackson.databind.node.BigIntegerNode

import play.api.libs.json.{ JsNumber, JsObject }

import JavaParsers._

object JavaParsersSpecs extends org.specs2.mutable.Specification {
  "Java parsers" title

  "JSON body" should {
    "be successfully parsed from integer value" in {
      val jsonNode =
        DefaultRequestBody(json = Some(JsNumber(new BigDecimal("50")))).asJson

      jsonNode.intValue must_== 50 and (jsonNode.asText must_== "50")
    }

    "be successfully parsed from float value" in {
      val bd = new BigDecimal("12.34")
      val jsonNode = DefaultRequestBody(json = Some(JsNumber(bd))).asJson

      jsonNode.decimalValue must_== bd and (jsonNode.asText must_== "12.34")
    }
  }
}
