package play.api.libs.openid

import java.net.URI
import java.net.URLEncoder

import java.util.regex.Pattern

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.ws._

class OpenID(val id: String) {
  var returnAction: String = ""
  var realmAction: String = ""
  var sregRequired = List[String]()
  var sregOptional = List[String]()
  var axRequired = Map[String, String]()
  var axOptional = Map[String, String]()

  def verify()(implicit request: RequestHeader): String = {
    try {
      // Normalize
      var claimedId = OpenID.normalize(id)
      var server: String = null
      var delegate: String = null

      // Discover
      val response = WS.url(claimedId).get().await.get

      // Try HTML
      val html: String = response.body
      server = OpenID.discoverServer(html)

      if (server == null) {
        // Try YADIS
        var xrds: scala.xml.Elem =
          if (response.header("Content-Type").contains("application/xrds+xml")) {
            response.xml
          } else if (response.header("X-XRDS-Location") != null) {
            WS.url(response.header("X-XRDS-Location")).get().await.get.xml
          } else {
            return null
          }
        for (serviceElem <- (xrds \\ "Service")) {
          if (!(serviceElem \ "Type" find { _.text == "http://specs.openid.net/auth/2.0/signon" }).isEmpty) {
            claimedId = (serviceElem \ "LocalID").text
            server = (serviceElem \ "URI").text
          } else if (!(serviceElem \ "Type" find { _.text == "http://specs.openid.net/auth/2.0/server" }).isEmpty) {
            server = (serviceElem \ "URI").text
          }
        }
        if (claimedId == null) {
          claimedId = "http://specs.openid.net/auth/2.0/identifier_select"
        }
        if (server == null) {
          return null
        }
      } else {
        // Delegate
        for (expr <- Array("<link[^>]+openid2[.]local_id[^>]+>", "<link[^>]+openid[.]delegate[^>]+>")) {
          val m = Pattern.compile(expr, Pattern.CASE_INSENSITIVE).matcher(html)
          if (m.find()) {
            delegate = OpenID.extractHref(m.group())
          }
        }
      }

      // Redirect
      var url: String = server
      if (!server.contains("?")) {
        url += "?"
      } else if (!url.endsWith("?") && !url.endsWith("&")) {
        url += "&"
      }
      val encode: String => String = URLEncoder.encode(_, "UTF-8")

      url += "openid.ns=" + encode("http://specs.openid.net/auth/2.0")
      url += "&openid.mode=checkid_setup"
      url += "&openid.claimed_id=" + encode(claimedId)
      url += "&openid.identity=" + encode(if (delegate == null) claimedId else delegate)

      if (returnAction != null && (returnAction.startsWith("http://") || returnAction.startsWith("https://"))) {
        url += "&openid.return_to=" + encode(returnAction)
      } else {
        url += "&openid.return_to=" + encode("http://" + request.host + returnAction)
      }
      if (realmAction != null && (realmAction.startsWith("http://") || realmAction.startsWith("https://"))) {
        url += "&openid.realm=" + encode(realmAction)
      } else {
        url += "&openid.realm=" + encode("http://" + request.host + realmAction)
      }

      if (!sregOptional.isEmpty || !sregRequired.isEmpty) {
        url += "&openid.ns.sreg=" + encode("http://openid.net/extensions/sreg/1.1")
      }
      if (!sregOptional.isEmpty) {
        url += "&openid.sreg.optional=" + (for (a <- sregOptional)
          yield encode(a)).mkString(",")
      }
      if (!sregRequired.isEmpty) {
        url += "&openid.sreg.required=" + (for (a <- sregRequired)
          yield encode(a)).mkString(",")
      }
      if (!axRequired.isEmpty || !axOptional.isEmpty) {
        url += "&openid.ns.ax=http%3A%2F%2Fopenid.net%2Fsrv%2Fax%2F1.0" +
          "&openid.ax.mode=fetch_request"
        if (!axRequired.isEmpty) {
          url += (for ((key, value) <- axRequired)
            yield "&openid.ax.type." + key + "=" + encode(value)).mkString +
            "&openid.ax.required=" + axRequired.keys.mkString(",")
        }
        if (!axOptional.isEmpty) {
          url += (for ((key, value) <- axOptional)
            yield "&openid.ax.type." + key + "=" + encode(value)).mkString +
            "&openid.ax.if_available=" + axOptional.keys.mkString(",")
        }
      }

      Logger("play").trace("Send OpenID request " + url)
      url
    } catch {
      case e: Exception => return null
    }
  }
}

object OpenID {

  def extractHref(link: String): String = {
    for (expr <- Array("href=\"([^\"]*)\"", "href=\'([^\']*)\'")) {
      val m = Pattern.compile(expr).matcher(link)
      if (m.find()) {
        return m.group(1).trim()
      }
    }
    null
  }

  def discoverServer(openID: String): String = {
    val openIDbody = if (openID.startsWith("http")) {
      WS.url(openID).get().await.get.body
    } else {
      openID
    }
    for (
      expr <- Array("<link[^>]+openid2[.]provider[^>]+>",
        "<link[^>]+openid[.]server[^>]+>")
    ) {
      val m = Pattern.compile(expr,
        Pattern.CASE_INSENSITIVE).matcher(openIDbody)
      if (m.find()) {
        return extractHref(m.group())
      }
    }
    null
  }

  def apply(id: String)(implicit request: RequestHeader): OpenID = {
    val openID = new OpenID(id)
    openID.returnAction = request.path
    openID.realmAction = request.path
    openID
  }

  /**
   * Is the current request an authentication response from the OP ?
   */
  def isAuthenticationResponse()(implicit request: RequestHeader): Boolean = {
    request.queryString.contains("openid.mode")
  }

  /**
   * Retrieve the verified OpenID
   * @return A UserInfo object
   */
  def getVerifiedID()(implicit request: RequestHeader): UserInfo = {
    try {
      val mode: String = request.queryString.get("openid.mode").flatMap(_.headOption).getOrElse(null)

      // Check authentication
      if (mode != "id_res")
        return null

      // id
      val id: String = normalize(request.queryString.getOrElse("openid.claimed_id",
          request.queryString("openid.identity")).head)

      // server
      val server = request.queryString.get("openid.op_endpoint").
        flatMap(_.headOption).getOrElse(discoverServer(id))

      val fields: String = request.rawQueryString.replace("openid.mode=id_res", "openid.mode=check_authentication")
      val response = WS.url(server).withHeaders(("Content-Type", "application/x-www-form-urlencoded")).
        post(fields).await.get
      if (!(response.status == 200 && response.body.contains("is_valid:true"))) {
        return null
      }
      var ext = Map[String, String]()
      for (
        (key, value) <- request.queryString;
        expr <- Array("^openid[.].+[.]value[.]([^.]+)([.]\\d+)?$",
          "^openid[.]sreg[.]([^.]+)$")
      ) {
        val m = Pattern.compile(expr).matcher(key)
        if (m.matches()) {
          ext += (m.group(1) -> value.head)
        }
      }
      UserInfo(id, ext)
    } catch {
      case e: Exception => throw new RuntimeException(e)
    }
  }

  /**
   * Normalize the given openid as a standard openid
   */
  def normalize(openID: String): String = {
    var result = openID.trim()
    if (!result.startsWith("http://") && !result.startsWith("https://")) {
      result = "http://" + result
    }
    try {
      val url = new URI(result)
      val frag = url.getRawFragment()
      if (frag != null && frag.length() > 0) {
        result = result.replace("#" + frag, "")
      }
      if (url.getPath().equals("")) {
        result += "/"
      }
      new URI(result).toString()
    } catch {
      case _ => throw new RuntimeException(openID + " is not a valid URL")
    }
  }
}

case class UserInfo(id: String, extensions: Map[String, String]) {
  override def toString(): String = {
    id + " " + extensions
  }
}
