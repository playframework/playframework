/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.api.http.{ HttpErrorHandler, DefaultHttpErrorHandler }
import play.api.mvc.Handler
import play.twirl.api.JavaScript

/**
 * Helper utilities related to `Router`.
 */
object Routes {

  // -- TAGS

  val ROUTE_VERB = "ROUTE_VERB"
  val ROUTE_PATTERN = "ROUTE_PATTERN"
  val ROUTE_CONTROLLER = "ROUTE_CONTROLLER"
  val ROUTE_ACTION_METHOD = "ROUTE_ACTION_METHOD"
  val ROUTE_COMMENTS = "ROUTE_COMMENTS"

  // --

  import play.core.Router._
  import play.api.mvc.RequestHeader

  /**
   * Helper to create a router from a partial function
   */
  def routes(routes: PartialFunction[RequestHeader, Handler]): SimpleRoutes = new SimpleRoutes(routes, "/")

  /**
   * A simple router that just wraps a partial function.
   *
   * @param suppliedRoutes The partial function that does the routing.
   * @param prefix The prefix at which the router lives.
   */
  class SimpleRoutes(suppliedRoutes: PartialFunction[RequestHeader, Handler],
      prefix: String) extends play.core.Router.Routes {
    def routes = {
      if (prefix == "/") {
        suppliedRoutes
      } else {
        val p = if (prefix.endsWith("/")) prefix else prefix + "/"
        val prefixed: PartialFunction[RequestHeader, RequestHeader] = {
          case rh: RequestHeader if rh.path.startsWith(p) => rh.copy(path = rh.path.drop(p.length - 1))
        }
        Function.unlift(prefixed.lift.andThen(_.flatMap(suppliedRoutes.lift)))
      }
    }
    def documentation = Nil
    def withPrefix(prefix: String) = new SimpleRoutes(suppliedRoutes, prefix)
    val errorHandler = DefaultHttpErrorHandler // This isn't used, so the default one is fine
  }

  /**
   * Generates a JavaScript router.
   *
   * For example:
   * {{{
   * Routes.javascriptRouter("MyRouter")(
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
  def javascriptRouter(name: String = "Router", ajaxMethod: Option[String] = Some("jQuery.ajax"))(routes: JavascriptReverseRoute*)(implicit request: RequestHeader): JavaScript = {
    javascriptRouter(name, ajaxMethod, request.host, routes: _*)
  }

  val jsReservedWords = Seq("break", "case", "catch", "continue", "debugger",
    "default", "delete", "do", "else", "finally", "for", "function", "if",
    "in", "instanceof", "new", "return", "switch", "this", "throw", "try",
    "typeof", "var", "void", "while", "with")

  // TODO: This JS needs to be re-written as it isn't easily maintained.
  def javascriptRouter(name: String, ajaxMethod: Option[String], host: String, routes: JavascriptReverseRoute*): JavaScript = JavaScript {
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
          route.name.split('.').map(name => if (jsReservedWords.contains(name)) { "['" + name + "']" } else { "." + name }).mkString("").tail,
          route.f)
      }.mkString("\n"),
      name)
  }

}
