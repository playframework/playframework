/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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

  def apply(name: String, ajaxMethod: Option[String], host: String, routes: JavaScriptReverseRoute*): JavaScript = JavaScript {
    import org.apache.commons.lang3.StringEscapeUtils.{ escapeEcmaScript => esc }
    val ajaxField = ajaxMethod.fold("")(m => s"ajax:function(c){c=c||{};c.url=r.url;c.type=r.method;return $m(c)},")
    val routesStr = routes.map { route =>
      val nameParts = route.name.split('.')
      val controllerName = nameParts.dropRight(1).mkString(".")
      val prop = "_root" + nameParts.map(p => s"['${esc(p)}']").mkString
      s"_nS('${esc(controllerName)}'); $prop = ${route.f};"
    }.mkString("\n")
    s"""
      |var $name = {}; (function(_root){
      |var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
      |var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
      |var _s = function(p,s){return p+((s===true||(s&&s.secure))?'s':'')+'://'}
      |var _wA = function(r){return {$ajaxField method:r.method,type:r.method,url:r.url,absoluteURL: function(s){return _s('http',s)+'${esc(host)}'+r.url},webSocketURL: function(s){return _s('ws',s)+'${esc(host)}'+r.url}}}
      |$routesStr
      |})($name)
    """.stripMargin.trim
  }
}
