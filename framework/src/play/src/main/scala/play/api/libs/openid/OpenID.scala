package play.api.libs.openid

import play.api.libs.concurrent.Promise
import play.api.libs.ws.WS
import scala.util.matching.Regex
import play.api.http.HeaderNames
import scala.xml.Elem
import java.net.URLEncoder
import play.api.libs.concurrent.PurePromise
import play.api.data.Form
import play.api.data.Forms.{ of, text, optional }
import play.api.mvc.Request

import play.api.libs.concurrent.execution.defaultContext

case class OpenIDServer(url: String, delegate: Option[String])

case class UserInfo(id: String, attributes: Map[String, String] = Map.empty)

/**
 * provides user information for a verified user
 */
object UserInfo {

  def apply(queryString: Map[String, Seq[String]]): UserInfo = {
    val extractor = new UserInfoExtractor(queryString)
    val id = extractor.id getOrElse(throw Errors.BAD_RESPONSE)
    new UserInfo(id, extractor.axAttributes)
  }

  /** Extract the values required to create an instance of the UserInfo
   *
   * The UserInfoExtractor ensures that attributes returned via OpenId attribute exchange are signed
   * (i.e. listed in the openid.signed field) and verified in the check_authentication step.
   */
  private[openid] class UserInfoExtractor(params:Map[String, Seq[String]]) {
    val AxAttribute =  """^openid\.([^.]+\.value\.([^.]+(\.\d+)?))$""".r
    val extractAxAttribute: PartialFunction[String, (String,String)] = {
      case AxAttribute(fullKey, key, num) => (fullKey, key) // fullKey e.g. 'ext1.value.email', shortKey e.g. 'email' or 'fav_movie.2'
    }

    private lazy val signedFields = params.get("openid.signed") flatMap {_.headOption map {_.split(",")}} getOrElse(Array())

    def id = params.get("openid.claimed_id").flatMap(_.headOption).orElse(params.get("openid.identity").flatMap(_.headOption))

    def axAttributes = params.foldLeft(Map[String,String]()) {
      case (result, (key, values)) => extractAxAttribute.lift(key) flatMap {
        case (fullKey, shortKey) if signedFields.contains(fullKey) => values.headOption map {value => Map(shortKey -> value)}
        case _ => None
      } map(result ++ _) getOrElse result
    }
  }

}
/**
 * provides OpenID support
 */
object OpenID {

  /**
   * Retrieve the URL where the user should be redirected to start the OpenID authentication process
   */
  def redirectURL(openID: String,
    callbackURL: String,
    axRequired: Seq[(String, String)] = Seq.empty,
    axOptional: Seq[(String, String)] = Seq.empty,
    realm: Option[String] = None): Promise[String] = {
    val claimedId = normalize(openID)
    discoverServer(claimedId).map(server => {
      val parameters = Seq(
        "openid.ns" -> "http://specs.openid.net/auth/2.0",
        "openid.mode" -> "checkid_setup",
        "openid.claimed_id" -> claimedId,
        "openid.identity" -> server.delegate.getOrElse(claimedId),
        "openid.return_to" -> callbackURL
      ) ++ axParameters(axRequired, axOptional) ++ realm.map("openid.realm" -> _).toList
      val separator = if (server.url.contains("?")) "&" else "?"
      server.url + separator + parameters.map(pair => pair._1 + "=" + URLEncoder.encode(pair._2, "UTF-8")).mkString("&")
    })
  }

  /**
   * From a request corresponding to the callback from the OpenID server, check the identity of the current user
   */
  def verifiedId(implicit request: Request[_]): Promise[UserInfo] = verifiedId(request.queryString)

  /**
   * For internal use
   */
  def verifiedId(queryString: java.util.Map[String, Array[String]]): Promise[UserInfo] = {
    import scala.collection.JavaConversions._
    verifiedId(queryString.toMap.mapValues(_.toSeq))
  }

  private def verifiedId(queryString: Map[String, Seq[String]]): Promise[UserInfo] = {
    (queryString.get("openid.mode").flatMap(_.headOption),
      queryString.get("openid.claimed_id").flatMap(_.headOption).orElse(queryString.get("openid.identity").flatMap(_.headOption)),
      queryString.get("openid.op_endpoint").flatMap(_.headOption)) match {
        case (Some("id_res"), Some(id), endPoint) => {
          val server: Promise[String] = endPoint.map(PurePromise(_)).getOrElse(discoverServer(id).map(_.url))
          server.flatMap(url => {
            val fields = (queryString - "openid.mode" + ("openid.mode" -> Seq("check_authentication")))
            WS.url(url).post(fields).map(response => {
              if (response.status == 200 && response.body.contains("is_valid:true")) {
                UserInfo(queryString)
              } else throw Errors.AUTH_ERROR
            })
          })
        }
        case _ => PurePromise(throw Errors.BAD_RESPONSE)
      }
  }

  private def axParameters(axRequired: Seq[(String, String)],
    axOptional: Seq[(String, String)]): Seq[(String, String)] = {
    if (axRequired.length == 0 && axOptional.length == 0)
      Nil
    else {
      val axRequiredParams = if (axRequired.size == 0) Nil
      else Seq("openid.ax.required" -> axRequired.map(_._1).mkString(","))

      val axOptionalParams = if (axOptional.size == 0) Nil
      else Seq("openid.ax.if_available" -> axOptional.map(_._1).mkString(","))

      val definitions = (axRequired ++ axOptional).map(attribute => ("openid.ax.type." + attribute._1 -> attribute._2))

      Seq("openid.ns.ax" -> "http://openid.net/srv/ax/1.0", "openid.ax.mode" -> "fetch_request") ++ axRequiredParams ++ axOptionalParams ++ definitions
    }
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
  private def discoverServer(openid: String): Promise[OpenIDServer] = {
    WS.url(openid).get().map(response => {
      // Try HTML
      val serverUrl: Option[String] = providerRegex.findFirstIn(response.body)
        .orElse(serverRegex.findFirstIn(response.body))
        .flatMap(extractHref(_))
      val fromHtml = serverUrl.map(url => {
        val delegate: Option[String] = localidRegex.findFirstIn(response.body)
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
    new Regex("""href="([^"]*)"""").findFirstMatchIn(link).map(_.group(1).trim).
      orElse(new Regex("""href='([^']*)'""").findFirstMatchIn(link).map(_.group(1).trim))

}

