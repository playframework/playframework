package play.api.mvc

import play.api.json._

import scala.xml._
import scala.collection.JavaConverters._

object JParsers extends BodyParsers {

  import play.mvc.Http.{ RequestBody }

  case class DefaultRequestBody(
    urlFormEncoded: Option[Map[String, Seq[String]]] = None,
    raw: Option[Array[Byte]] = None,
    text: Option[String] = None,
    json: Option[JsValue] = None,
    xml: Option[NodeSeq] = None) extends RequestBody {

    override lazy val asUrlFormEncoded = {
      urlFormEncoded.map(_.mapValues(_.toArray).asJava).orNull
    }

    override def asRaw = {
      raw.orNull
    }

    override def asText = {
      text.orNull
    }

    override lazy val asJson = {
      import org.codehaus.jackson._
      import org.codehaus.jackson.map._

      json.map { json =>
        new ObjectMapper().readValue(json.toString, classOf[JsonNode])
      }.orNull
    }

    override lazy val asXml = {
      xml.map { xml =>
        play.libs.XML.fromString(xml.toString)
      }.orNull
    }

  }

  def anyContent: BodyParser[RequestBody] = parse.anyContent.map { anyContent =>
    DefaultRequestBody(
      anyContent.asUrlFormEncoded,
      anyContent.asRaw,
      anyContent.asText,
      anyContent.asJson,
      anyContent.asXml)
  }

  def json: BodyParser[RequestBody] = parse.json.map { json =>
    DefaultRequestBody(json = Some(json))
  }

  def tolerantJson: BodyParser[RequestBody] = parse.tolerantJson.map { json =>
    DefaultRequestBody(json = Some(json))
  }

  def xml: BodyParser[RequestBody] = parse.xml.map { xml =>
    DefaultRequestBody(xml = Some(xml))
  }

  def tolerantXml: BodyParser[RequestBody] = parse.tolerantXml.map { xml =>
    DefaultRequestBody(xml = Some(xml))
  }

  def text: BodyParser[RequestBody] = parse.text.map { text =>
    DefaultRequestBody(text = Some(text))
  }

  def tolerantText: BodyParser[RequestBody] = parse.tolerantText.map { text =>
    DefaultRequestBody(text = Some(text))
  }

  def urlFormEncoded: BodyParser[RequestBody] = parse.urlFormEncoded.map { urlFormEncoded =>
    DefaultRequestBody(urlFormEncoded = Some(urlFormEncoded))
  }

  def raw: BodyParser[RequestBody] = parse.raw.map { raw =>
    DefaultRequestBody(raw = Some(raw))
  }

}