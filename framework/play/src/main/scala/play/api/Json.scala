package play.api.json

import org.codehaus.jackson.map.module.SimpleModule
import org.codehaus.jackson.Version
import org.codehaus.jackson.map.Module.SetupContext

import AST._
import Formats._

object Json extends com.codahale.jerkson.Json {

  private object module extends SimpleModule("PlayJson", Version.unknownVersion()) {
    override def setupModule(context: SetupContext) {
      context.addDeserializers(new PlayDeserializers(classLoader))
      context.addSerializers(new PlaySerializers)
    }
  }
  mapper.registerModule(module)

  def Js(input: String): JsValue = parse[JsValue](input)

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

