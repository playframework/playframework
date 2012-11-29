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

    implicit val cookieReads = Json.reads[Cookie]
    implicit val cookieWrites = Json.writes[Cookie]

    implicit val echoReads = Json.reads[Echo]
    implicit val echoWrites = Json.writes[Echo]

    implicit val toReturnReads = Json.reads[ToReturn]
    implicit val toReturnWrites = Json.writes[ToReturn]
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
