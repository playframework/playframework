package play.api.libs.openid

import org.specs2.mutable.Specification

object UserInfoSpec extends Specification {

  val claimedId = "http://example.com/openid?id=C123"
  val identity = "http://example.com/openid?id=C123&id"
  val defaultSigned = "op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle"

  "UserInfo" should {
    "successfully be created using the value of the openid.claimed_id field" in {
      val userInfo = UserInfo(createDefaultResponse)
      userInfo.id must be equalTo claimedId
      userInfo.attributes must beEmpty
    }
    "successfully be created using the value of the openid.identity field" in {
      // For testing the claimed_id is removed to check that id contains the identity value.
      val userInfo = UserInfo(createDefaultResponse - "openid.claimed_id")
      userInfo.id must be equalTo identity
      userInfo.attributes must beEmpty
    }
  }

  "UserInfo" should {
    "not include attributes that are not signed" in {
      val requestParams = createDefaultResponseWithAttributeExchange ++ Map[String, Seq[String]](
        "openid.ext1.type.email" -> "http://schema.openid.net/contact/email",
        "openid.ext1.value.email" -> "user@example.com",
        "openid.signed" -> defaultSigned) // the email attribute is not in the list of signed fields
      val userInfo = UserInfo(requestParams)
      userInfo.attributes.get("email") must beNone
    }

    "include attributes that are signed" in {
      val requestParams = createDefaultResponseWithAttributeExchange ++ Map[String, Seq[String]](
        "openid.ext1.type.email" -> "http://schema.openid.net/contact/email",
        "openid.ext1.value.email" -> "user@example.com", // the email attribute *is* in the list of signed fields
        "openid.signed" -> (defaultSigned + "ns.ext1,ext1.mode,ext1.type.email,ext1.value.email"))
      val userInfo = UserInfo(requestParams)
      userInfo.attributes.get("email") must beSome("user@example.com")
    }

    "include multi valued attributes that are signed" in {
      val requestParams = createDefaultResponseWithAttributeExchange ++ Map[String, Seq[String]](
        "openid.ext1.type.fav_movie" -> "http://example.com/schema/favourite_movie",
        "openid.ext1.count.fav_movie" -> "2",
        "openid.ext1.value.fav_movie.1" -> "Movie1",
        "openid.ext1.value.fav_movie.2" -> "Movie2",
        "openid.signed" -> (defaultSigned + "ns.ext1,ext1.mode,ext1.type.fav_movie,ext1.value.fav_movie.1,ext1.value.fav_movie.2,ext1.count.fav_movie"))
      val userInfo = UserInfo(requestParams)
      userInfo.attributes.size must be equalTo 2
      userInfo.attributes.get("fav_movie.1") must beSome("Movie1")
      userInfo.attributes.get("fav_movie.2") must beSome("Movie2")
    }
  }

  "only include attributes that have a value" in {
    val requestParams = createDefaultResponseWithAttributeExchange ++ Map[String, Seq[String]](
      "openid.ext1.type.firstName" -> "http://axschema.org/namePerson/first",
      "openid.ext1.value.firstName" -> Nil,
      "openid.signed" -> (defaultSigned + "ns.ext1,ext1.mode,ext1.type.email,ext1.value.email,ext1.type.firstName,ext1.value.firstName"))
    val userInfo = UserInfo(requestParams)
    userInfo.attributes.get("firstName") must beNone
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
    "openid.signed" -> defaultSigned,
    "openid.sig" -> "MWRsJZ/9AOMQt9gH6zTZIfIjk6g="
  )

  // http://openid.net/specs/openid-attribute-exchange-1_0.html#fetch_response
  private def createDefaultResponseWithAttributeExchange = Map[String, Seq[String]](
    "openid.ns.ext1" -> "http://openid.net/srv/ax/1.0",
    "openid.ext1.mode" -> "fetch_response"
  ) ++ createDefaultResponse

  private implicit def stringToSeq(s: String): Seq[String] = Seq(s)
}
