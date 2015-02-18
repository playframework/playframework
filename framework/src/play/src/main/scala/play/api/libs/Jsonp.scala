/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import play.api.libs.json.JsValue
import play.api.http.{ ContentTypeOf, ContentTypes, Writeable }
import play.api.mvc.Codec

/**
 * JSONP helper.
 *
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
 *
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

  implicit def contentTypeOf_Jsonp(implicit codec: Codec): ContentTypeOf[Jsonp] = {
    ContentTypeOf[Jsonp](Some(ContentTypes.JAVASCRIPT))
  }

  implicit def writeableOf_Jsonp(implicit codec: Codec): Writeable[Jsonp] = Writeable { jsonp =>
    codec.encode("%s(%s);".format(jsonp.padding, jsonp.json))
  }

}
