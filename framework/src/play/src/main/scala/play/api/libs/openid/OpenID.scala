package play.api.libs.openid

import play.api.libs.concurrent.Promise
import play.api.libs.ws.WS
import scala.util.matching.Regex
import play.api.http.HeaderNames
import scala.xml.Elem
import java.net.URLEncoder
import play.api.libs.concurrent.PurePromise
import play.api.data.Form
import play.api.data.Forms.{of, text, optional}
import play.api.mvc.Request

case class OpenIDServer(url: String, delegate: Option[String])

case class UserInfo(id: String)

object OpenID {

  /**
   * Retrieve the URL where the user should be redirected to start the OpenID authentication process
   */
  def redirectURL(openID: String, callbackURL: String): Promise[String] = {
    val claimedId = normalize(openID)
    discoverServer(claimedId).map( server => {
      val parameters = Seq(
        "openid.ns" -> "http://specs.openid.net/auth/2.0",
        "openid.mode" -> "checkid_setup",
        "openid.claimed_id" -> claimedId,
        "openid.identity" -> server.delegate.getOrElse(claimedId),
        "openid.return_to" -> callbackURL
      )
      val separator = if (server.url.contains("?")) "&" else "?"
      server.url + separator + parameters.map(pair => pair._1 + "=" + URLEncoder.encode(pair._2, "UTF-8")).mkString("&")
    })
  }

  /**
   * From a request corresponding to the callback from the OpenID server, check the identity of the current user
   */
  def verifiedId(implicit request: Request[_]): Promise[UserInfo] = {
    Form(of(
      "openid.mode" -> text,
      "openid.claimed_id" -> optional(text),
      "openid.identity" -> optional(text),
      "openid.op_endpoint" -> optional(text)
    )).bindFromRequest.fold(
        error => PurePromise(throw Errors.MISSING_PARAMETERS),
        {
          case ("id_res", claimedId, identity, endPoint) => {
            claimedId.orElse(identity).map( id => {
              val server: Promise[String] = endPoint.map(PurePromise(_)).getOrElse(discoverServer(id).map(_.url))
              server.flatMap( url => {
                val fields = request.queryString - "openid.mode" + ("openid.mode" -> Seq("check_authentication"))
                WS.url(url).post(fields).map(response => {
                  if (response.status == 200 && response.body.contains("is_valid:true"))
                    UserInfo(id)
                  else
                    throw Errors.AUTH_ERROR
                })
              })
            }).getOrElse(PurePromise(throw Errors.BAD_RESPONSE))
          }
        }
    )
  }

  private def normalize(openID: String): String = {
    val trimmed = openID.trim
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
      "http://" + trimmed
    } else {
      trimmed
    }
  }

  private val providerRegex = new Regex("""<link[^>]+openid2[.]provider[^>]+>""")
  private val serverRegex = new Regex("""<link[^>]+openid[.]server[^>]+>""")
  private val localidRegex = new Regex("""<link[^>]+openid2[.]local_id[^>]+>""")
  private val delegateRegex = new Regex("""<link[^>]+openid[.]delegate[^>]+>""")

  /**
   * Resolve the OpenID server from the user's OpenID
   */
  private def discoverServer(openid: String):Promise[OpenIDServer] = {
    WS.url(openid).get().map(response => {
      // Try HTML
      val serverUrl:Option[String] = providerRegex.findFirstIn(response.body)
          .orElse(serverRegex.findFirstIn(response.body))
          .flatMap(extractHref(_))
      val fromHtml = serverUrl.map(url => {
        val delegate:Option[String] = localidRegex.findFirstIn(response.body)
          .orElse(delegateRegex.findFirstIn(response.body)).flatMap(extractHref(_))
        OpenIDServer(url, delegate)
      })
      // Try XRD
      val fromXRD = response.header(HeaderNames.CONTENT_TYPE).filter(_.contains("application/xrds+xml")).map(_ =>
        OpenIDServer((response.xml \\ "URI").text, None)
      )
      fromHtml.orElse(fromXRD).getOrElse(throw Errors.NETWORK_ERROR)
    })
  }

  private def extractHref(link: String): Option[String] =
    new Regex("""href="([^"]*)"""").findFirstMatchIn(link).map(_.group(1).trim)

}
