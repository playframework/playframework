package play.api.libs

import openid.UserInfo
import org.specs2.mutable.Specification
import scala.Predef._

object OpenIDSpec extends Specification {

  val claimedId = "http://example.com/openid?id=C123"
  val identity = "http://example.com/openid?id=C123&id"

  "UserInfo" should {
    "successfully be created for the default response using the value of the openid.claimed_id field" in {
      val userInfo = UserInfo(createDefaultResponse)
      userInfo.id must be equalTo claimedId
      userInfo.attributes must beEmpty
    }
    "successfully be created for the default response using the value of the openid.identity field" in {
      // For testing the claimed_id is removed to check that id contains the identity value.
      val userInfo = UserInfo(createDefaultResponse - "openid.claimed_id")
      userInfo.id must be equalTo identity
      userInfo.attributes must beEmpty
    }
  }

  // See 10.1 - Positive Assertions
  // http://openid.net/specs/openid-authentication-2_0.html#positive_assertions
  private def createDefaultResponse: Map[String, Seq[String]] = Map(
    "openid.ns" -> "http://specs.openid.net/auth/2.0",
    "openid.mode" -> "id_res",
    "openid.op_endpoint" -> "https://www.google.com/a/example.com/o8/ud?be=o8",
    "openid.claimed_id" -> claimedId,
    "openid.identity" -> identity,
    "openid.return_to" -> "https://example.com/openid?abc=false",
    "openid.response_nonce" -> "2012-05-25T06:47:55ZEJvRv76xQcWbTG",
    "openid.assoc_handle" -> "AMlYA9VC8_UIj4-y4K_X2E_mdv-123-ABC",
    "openid.signed" -> "op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle",
    "openid.sig" -> "MWRsJZ/9AOMQt9gH6zTZIfIjk6g="
  )

  private implicit def stringToSeq(s: String): Seq[String] = Seq(s)
}
