package play.api.libs

import play.api.libs.json.JsValue
<<<<<<< .merge_file_501xFf
import play.api.http.{ ContentTypeOf, ContentTypes, Writeable }
=======
import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
>>>>>>> .merge_file_o0wnCC
import play.api.mvc.Codec

/**
 * JSONP helper.
<<<<<<< .merge_file_501xFf
 *
=======
 * 
>>>>>>> .merge_file_o0wnCC
 * Example of use, provided the following route definition:
 * {{{
 *   GET  /my-service       Application.myService(callback: String)
 * }}}
 * The following action definition:
 * {{{
 *   def myService(callback: String) = Action {
 *     val json = ...
 *     Ok(Jsonp(callback, json))
 *   }
 * }}}
 * And the following request:
 * {{{
 *   GET /my-service?callback=foo
 * }}}
 * The response will have content type “text/javascript” and will look like the following:
 * {{{
 *   foo({...});
 * }}}
<<<<<<< .merge_file_501xFf
 *
=======
 * 
>>>>>>> .merge_file_o0wnCC
 * Another example, showing how to serve either JSON or JSONP from the same action, according to the presence of
 * a “callback” parameter in the query string:
 * {{{
 *   def myService = Action { implicit request =>
 *     val json = ...
 *     request.queryString.get("callback").flatMap(_.headOption) match {
 *       case Some(callback) => Ok(Jsonp(callback, json))
 *       case None => Ok(json)
 *     }
 *   }
 * }}}
 */
case class Jsonp(padding: String, json: JsValue)

object Jsonp {

<<<<<<< .merge_file_501xFf
  implicit def contentTypeOf_Jsonp(implicit codec: Codec): ContentTypeOf[Jsonp] = {
    ContentTypeOf[Jsonp](Some(ContentTypes.JAVASCRIPT))
  }

  implicit def writeableOf_Jsonp(implicit codec: Codec): Writeable[Jsonp] = Writeable { jsonp =>
=======
  implicit val contentTypeOf_Jsonp = ContentTypeOf[Jsonp](Some(ContentTypes.JAVASCRIPT))

  implicit def writeableOf_Jsonp(implicit codec: Codec) = Writeable[Jsonp] { jsonp =>
>>>>>>> .merge_file_o0wnCC
    codec.encode("%s(%s);".format(jsonp.padding, jsonp.json))
  }

}