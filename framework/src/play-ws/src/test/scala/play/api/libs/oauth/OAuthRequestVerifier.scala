/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.oauth

import play.api.mvc.RequestHeader
import play.core.parsers.FormUrlEncodedParser
import java.util.Locale
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.apache.commons.codec.binary.Base64
import org.specs2.matcher.MustExpectations._
import org.specs2.matcher.Matchers._

import org.specs2.matcher.MatchResult

/**
 * Verifies OAuth requests
 */
object OAuthRequestVerifier {

  def percentDecode(input: String): String = {
    if (input == null) {
      ""
    } else {
      java.net.URLDecoder.decode(input, "UTF-8")
    }
  }

  def percentEncode(input: String): String = {
    if (input == null) {
      ""
    } else {
      java.net.URLEncoder.encode(input, "UTF-8")
        .replace("+", "%20")
        .replace("*", "%2A")
        .replace("%7E", "~")
    }
  }

  /**
   * Verify that the given request is a valid OAuth request given the consumer key and request token
   */
  def verifyRequest(request: RequestHeader, body: Array[Byte], hostUrl: String, consumerKey: ConsumerKey,
    requestToken: RequestToken): MatchResult[_] = {
    val method = request.method
    val baseUrl = hostUrl + request.path

    request.headers.get("Authorization") must beSome.like {
      case authorization =>
        authorization must startWith("OAuth ")
        val oauthParams = authorization.drop(6).split(", ").map { param =>
          val splitted = param.split("=")
          val key = percentDecode(splitted(0))
          val rawValue = splitted(1)
          rawValue must startWith("\"")
          rawValue must endWith("\"")
          val value = percentDecode(rawValue.drop(1).dropRight(1))
          key -> value
        }

        val oauthParamsMap = oauthParams.toMap
        val oauthSignature = oauthParamsMap.get("oauth_signature")
        val oauthToken = oauthParamsMap.get("oauth_token")
        val oauthConsumerKey = oauthParamsMap.get("oauth_consumer_key")
        val oauthSignatureMethod = oauthParamsMap.get("oauth_signature_method")
        val oauthTimestamp = oauthParamsMap.get("oauth_timestamp")

        // Verify various fields
        oauthToken must beSome(requestToken.token)
        oauthConsumerKey must beSome(consumerKey.key)
        oauthSignatureMethod must beSome("HMAC-SHA1")
        oauthTimestamp must beSome.like {
          case timestamp =>
            // Verify no more than 100 seconds in the past
            timestamp.toLong must beGreaterThan(System.currentTimeMillis() / 1000 - 100)
        }

        // Verify the signature
        val collectedParams = oauthParams.filterNot(_._1 == "oauth_signature") ++ request.queryString.toSeq.flatMap {
          case (key, values) => values.map(value => key -> value)
        }
        // If the body is form URL encoded, must include body parameters
        val collectedParamsWithBody = request.contentType match {
          case Some(formUrlEncoded) if formUrlEncoded.startsWith("application/x-www-form-urlencoded") =>
            val form = FormUrlEncodedParser.parse(new String(body, "UTF-8")).toSeq.flatMap {
              case (key, values) => values.map(value => key -> value)
            }
            collectedParams ++ form
          case _ => collectedParams
        }
        oauthSignature must beSome.like {
          case signature =>
            val ourSignature = signParams(method, baseUrl, collectedParamsWithBody, consumerKey.secret, requestToken.secret)
            signature must_== ourSignature
        }
    }

  }

  def signParams(method: String, baseUrl: String, params: Seq[(String, String)], consumerSecret: String, tokenSecret: String): String = {
    // See https://dev.twitter.com/docs/auth/creating-signature

    // Params must be percent encoded before they are sorted
    val parameterString = params.map {
      case (key, value) => percentEncode(key) -> percentEncode(value)
    }.sorted.map {
      case (key, value) => s"$key=$value"
    }.mkString("&")

    val signatureBaseString = s"${method.toUpperCase(Locale.ENGLISH)}&${percentEncode(baseUrl)}&${percentEncode(parameterString)}"

    val signingKey = s"${percentEncode(consumerSecret)}&${percentEncode(tokenSecret)}"

    val keySpec = new SecretKeySpec(signingKey.getBytes("US-ASCII"), "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(keySpec)
    val signature = mac.doFinal(signatureBaseString.getBytes("US-ASCII"))
    new String(Base64.encodeBase64(signature), "US-ASCII")
  }

}
