/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.routing

import play.api.mvc.RequestHeader
import play.twirl.api.JavaScript

/**
 * A JavaScript reverse route
 */
case class JavaScriptReverseRoute(name: String, f: String)

object JavaScriptReverseRouter {

  /**
   * Generates a JavaScript router.
   *
   * For example:
   * {{{
   * JavaScriptReverseRouter("MyRouter")(
   *   controllers.routes.javascript.Application.index,
   *   controllers.routes.javascript.Application.list,
   *   controllers.routes.javascript.Application.create
   * )
   * }}}
   *
   * And then you can use the JavaScript router as:
   * {{{
   * var routeToHome = MyRouter.controllers.Application.index()
   * }}}
   *
   * @param name the JavaScript object name
   * @param routes the routes to include in this JavaScript router
   * @return the JavaScript code
   */
  def apply(name: String = "Router", ajaxMethod: Option[String] = Some("jQuery.ajax"))(routes: JavaScriptReverseRoute*)(implicit request: RequestHeader): JavaScript = {
    apply(name, ajaxMethod, request.host, routes: _*)
  }

  private val jsReservedWords = Seq("break", "case", "catch", "continue", "debugger",
    "default", "delete", "do", "else", "finally", "for", "function", "if",
    "in", "instanceof", "new", "return", "switch", "this", "throw", "try",
    "typeof", "var", "void", "while", "with")

  def apply(name: String, ajaxMethod: Option[String], host: String, routes: JavaScriptReverseRoute*): JavaScript = JavaScript {
    """|var %s = {}; (function(_root){
      |var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
      |var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
      |var _s = function(p,s){return p+((s===true||(s&&s.secure))?'s':'')+'://'}
      |var _wA = function(r){return {%s method:r.method,type:r.method,url:r.url,absoluteURL: function(s){return _s('http',s)+'%s'+r.url},webSocketURL: function(s){return _s('ws',s)+'%s'+r.url}}}
      |%s
      |})(%s)
    """.stripMargin.format(
      name,
      ajaxMethod.map("ajax:function(c){c=c||{};c.url=r.url;c.type=r.method;return " + _ + "(c)},").getOrElse(""),
      host,
      host,
      routes.map { route =>
        "_nS('%s'); _root.%s = %s".format(
          route.name.split('.').dropRight(1).mkString("."),
          route.name.split('.').map(name => if (jsReservedWords.contains(name)) {
            "['" + name + "']"
          } else {
            "." + name
          }).mkString("").tail,
          route.f)
      }.mkString("\n"),
      name)
  }
}