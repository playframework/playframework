package play.api {

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
    def javascriptRouter(name: String = "Router", ajaxMethod: String = "$.ajax")(routes: JavascriptReverseRoute*)(implicit request: RequestHeader): String = {
      javascriptRouter(name, ajaxMethod, request.host, routes: _*)
    }

    def javascriptRouter(name: String, ajaxMethod: String, host: String, routes: JavascriptReverseRoute*): String = {
      """|var %s = {}; (function(_root){
             |if(typeof($) == "undefined"){var $={ajax:function(p){var xhr=new XMLHttpRequest();xhr.onreadystatechange=function(){if(xhr.readyState<4)return;if(xhr.status!==200&&p.error)p.error(xhr,xhr.status);if(xhr.status==200&&p.success)p.success(xhr.response,xhr.status,xhr);};for(k in p.data){var d=[];d.push(k+"="+encodeURIComponent(p.data[k]))};xhr.open(p.type,p.url+(p.type=="GET"?("?"+d.join("&")):""),true);xhr.send(p.type!="GET"?d.join("&"):"");return xhr;}};}
             |var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
             |var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
             |var _s = function(p,s){return p+((s===true||(s&&s.secure))?'s':'')+'://'}
             |var _wA = function(r){return {ajax:function(c){c=c||{};c.url=r.url;c.type=r.method;return %s(c)}, method:r.method,url:r.url,absoluteURL: function(s){return _s('http',s)+'%s'+r.url},webSocketURL: function(s){return _s('ws',s)+'%s'+r.url}}}
             |%s
             |})(%s)
          """.stripMargin.format(
        name,
        ajaxMethod,
        host,
        host,
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

}
