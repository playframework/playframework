package play.api

/**
 * Helper utilities related to Router.
 */
object Routes {

  import play.core.Router._

  /**
   * Generate a Javascript router.
   *
   * Example:
   * {{{
   * Routes.javascriptRouter("MyRouter")(
   *   controllers.routes.javascript.Application.index,
   *   controllers.routes.javascript.Application.list,
   *   controllers.routes.javascript.Application.create
   * )
   * }}}
   *
   * And then you can use the Javascript router as:
   * {{{
   * var routeToHome = MyRouter.controllers.Application.index()
   * }}}
   *
   * @param name The Javascript object name.
   * @param routes The routes to include in this javascript router.
   * @return The javascript code.
   */
  def javascriptRouter(name: String = "Router")(routes: JavascriptReverseRoute*) = {
    """|var %s = {}; (function(_root){
           |var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
           |var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
           |var _wA = function(r){return {method:r.method,url:r.url,ajax:function(c){c.url=r.url;c.type=r.method;c.method=$.ajax(c)}}}
           |%s   
           |})(%s)
        """.stripMargin.format(
      name,
      routes.map { route =>
        "_nS('%s'); _root.%s = %s".format(
          route.name.split('.').dropRight(1).mkString("."),
          route.name,
          route.f,
          route.name)
      }.mkString("\n"),
      name)
  }

}