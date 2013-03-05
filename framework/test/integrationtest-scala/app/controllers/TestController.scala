package controllers

import play.api.mvc._
import play.api.libs.json._

/**
 * Used for testing stuff.
 */
object TestController extends Controller {

  object Model {
    import play.api.libs.functional.syntax._
    import org.apache.commons.codec.binary.Base64
    import play.api.data.validation.ValidationError

    case class Echo(method: String,
                    version: String,
                    body: Option[Array[Byte]],
                    headers: Map[String, Seq[String]],
                    session: Map[String, String],
                    flash: Map[String, String],
                    remoteAddress: String,
                    queryString: Map[String, Seq[String]],
                    uri: String,
                    path: String)

    case class ToReturn(status: Int = 200,
                        body: Option[Array[Byte]] = None,
                        headers: Map[String, String] = Map(),
                        cookies: List[Cookie] = List(),
                        session: Map[String, String] = Map(),
                        flash: Map[String, String] = Map())

    implicit val byteArrayWrites = new Writes[Array[Byte]] {
      def writes(o: Array[Byte]) = JsString(new String((Base64.encodeBase64(o))))
    }

    implicit val byteArrayReads = new Reads[Array[Byte]] {
      def reads(json: JsValue) = json match {
        case JsString(value) => JsSuccess(Base64.decodeBase64(value.getBytes))
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
      }
    }
//  case class Cookie(name: String, value: String, maxAge: Option[Int] = None, path: String = "/", 
  //domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)

    implicit val cookieReads = new Reads[Cookie] {
      def reads(json: JsValue) = JsSuccess(Cookie(
          (json \ "name").as[String],
          (json \ "value").as[String],
          (json \ "maxAge").asOpt[Int],
          (json \ "path").as[String],
          (json \ "domain").asOpt[String],
          (json \ "secure").as[Boolean],
          (json \ "httpOnly").as[Boolean]
        ))
    }
    implicit val cookieWrites = new Writes[Cookie] {
      def writes(c: Cookie) = JsObject(Seq())
    }

    implicit val echoReads = new Reads[Echo] {
      def reads(json: JsValue) = JsSuccess(Echo(
        (json \ "method").as[String],
        (json \ "version").as[String],
        (json \ "body").asOpt[Array[Byte]],
        (json \ "headers").as[Map[String, Seq[String]]],
        (json \ "session").as[Map[String, String]],
        (json \ "flash").as[Map[String, String]],
        (json \ "remoteAddress").as[String],
        (json \ "queryString").as[Map[String, Seq[String]]],
        (json \ "uri").as[String],
        (json \ "path").as[String]
      ))
    }
    implicit val echoWrites = new Writes[Echo] {
      def writes(e: Echo) = JsObject(Seq())
    }

    implicit val toReturnReads = new Reads[ToReturn]{
      def reads(json: JsValue) = JsSuccess(ToReturn(
        (json \ "status").as[Int],
        (json \ "body").asOpt[Array[Byte]],
        (json \ "headers").as[Map[String, String]],
        (json \ "cookies").as[List[Cookie]],
        (json \ "session").as[Map[String, String]],
        (json \ "flash").as[Map[String, String]]
      ))
    }
    implicit val toReturnWrites = new Writes[ToReturn] {
      import Json._
      def writes(t: ToReturn) = toJson(
        Map(
          "status" -> toJson(t.status),
          "body" -> toJson(t.body),
          "headers" -> toJson(t.headers),
          "cookies" -> toJson(t.cookies),
          "session" -> toJson(t.session),
          "flash" -> toJson(t.flash)
        )
      )
    }
  }

  import Model._

  def echo = Action(parse.raw) { request =>
    import request._
    Ok(Json.toJson(Echo(method, version, body.asBytes(), headers.toMap, request.session.data, request.flash.data, remoteAddress,
      queryString, uri, path)))
  }

  def slave = Action(parse.json) { request =>
    Json.fromJson[ToReturn](request.body).fold({ errors =>
      BadRequest(errors.toString())
    }, { toReturn =>
      toReturn.body.map(body => Status(toReturn.status)(body)).getOrElse(Status(toReturn.status))
        .withHeaders(toReturn.headers.toSeq:_*)
        .withCookies(toReturn.cookies:_*)
        .withSession(toReturn.session.foldLeft(request.session)((s, item) => s + item))
        .flashing(toReturn.flash.toSeq:_*)
    })
  }

}
