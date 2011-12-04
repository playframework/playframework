package play.api

import org.codehaus.jackson.map.module.SimpleModule
import org.codehaus.jackson.Version
import org.codehaus.jackson.map.Module.SetupContext

import json._
import json.AST._
import json.Formats._

object Json extends com.codahale.jerkson.Json {

  private object JerksonJson extends com.codahale.jerkson.Json {

    object module extends SimpleModule("PlayJson", Version.unknownVersion()) {
      override def setupModule(context: SetupContext) {
        context.addDeserializers(new PlayDeserializers(classLoader))
        context.addSerializers(new PlaySerializers)
      }
    }
    mapper.registerModule(module)

  }

  def parse(input: String): JsValue = JerksonJson.parse[JsValue](input)

  def stringify(json: JsValue): String = JerksonJson.generate(json)

  def tojson[T](o: T)(implicit tjs: Writes[T]): JsValue = tjs.writes(o)

  def fromjson[T](json: JsValue)(implicit fjs: Reads[T]): T = fjs.reads(json)

  trait Writes[T] {
    def writes(o: T): JsValue
  }

  trait Reads[T] {
    def reads(json: JsValue): T
  }

  trait Format[T] extends Writes[T] with Reads[T]

}

