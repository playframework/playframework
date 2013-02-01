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
     * Generates a JavaScript reverse router.
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
     * var routeToHome = MyRouter.controllers.Application.index();
     * }}}
     *
     * @param name the JavaScript object name
     * @param ajaxMethod set it to `true` if you want to generate an `ajax` method to each route (defaults to `false`)
     * @param routes the routes to include in this JavaScript reverse router
     * @return the JavaScript code
     */
    def javascriptRouter(name: String = "Router", ajaxMethod: Boolean = false)(routes: JavascriptReverseRoute*)(implicit request: RequestHeader): String = {
      javascriptRouter(name, ajaxMethod, request.host, routes: _*)
    }
    
    def javascriptRouter(name: String, ajaxMethod: Boolean, host: String, routes: JavascriptReverseRoute*): String = {
      val ajax = if (ajaxMethod) {
        Some("""|function(s){
           | s=s||{};var xhr = new XMLHttpRequest();xhr.open(this.method,this.url);
           | xhr.onreadystatechange = function(e){
           |  if(xhr.readyState===XMLHttpRequest.DONE){
           |   switch(Math.floor(xhr.status / 100)){
           |    case 2:
           |    case 3:
           |      s.success && s.success(xhr.responseText, xhr);
           |      break;
           |    default:
           |      s.error && s.error(xhr);
           |   }
           |  }
           | };
           | var d = null;
           | if(s.data){
           |  d = new FormData;
           |  for(var p in s.data){
           |   if(s.data.hasOwnProperty(p)) d.append(p, s.data[p]);
           |  }
           | }
           | xhr.send(d);
           | return xhr
           |}""".stripMargin)
      } else None
      javascriptRouter(name, ajax, host, routes: _*)
    }
    
    /**
     * Generates a JavaScript reverse router with a custom `ajax` method.
     *
     * For example, using jQuery:
     * {{{
     * Routes.javascriptRouter("MyRouter", ajaxMethod = "function (s) { s.url = this.url; s.type = this.method; return $.ajax(s) }")(
     *   controllers.routes.javascript.Application.index,
     *   controllers.routes.javascript.Application.list,
     *   controllers.routes.javascript.Application.create
     * )
     * }}}
     *
     * The generated reverse router adds an `ajax` method to the routes so that you can directly make Ajax calls:
     * {{{
     * MyRouter.controllers.Application.list().ajax({
     *   success: function (data) {
     *     console.log(data);
     *   }
     * });
     *
     * The `ajaxMethod` parameter is a JavaScript function that will be called when calling the `ajax` method of a route,
     * with the same parameters and with `this` bound to the route object.
     * }}}
     *
     * @param name the JavaScript object name
     * @param ajaxMethod a JavaScript function that will be called by the `ajax` method of routes objects
     * @param routes the routes to include in this JavaScript reverse router
     * @return the JavaScript code
     */
     def javascriptRouter(name: String, ajaxMethod: String)(routes: JavascriptReverseRoute*)(implicit request: RequestHeader): String = {
      javascriptRouter(name, Some(ajaxMethod), request.host, routes: _*)
    }

    def javascriptRouter(name: String, ajaxMethod: Option[String], host: String, routes: JavascriptReverseRoute*): String = {
      val (ajaxField, ajaxFn) = ajaxMethod match {
        case Some(fn) => ("ajax: function(){return _aj.apply(this,arguments)},", s"var _aj = $fn;")
        case None => ("","")
      }
      val routeDefs = routes.map { route =>
        val ns = route.name.split('.').dropRight(1).mkString(".")
        s"_nS('$ns'); _root.${route.name} = ${route.f}"
      }.mkString("\n")

      s"""|var $name = {}; (function(_root){
             |var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
             |var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
             |var _s = function(p,s){return p+((s===true||(s&&s.secure))?'s':'')+'://'}
             |$ajaxFn
             |var _wA = function(r){return {${ajaxField}method:r.method,url:r.url,absoluteURL: function(s){return _s('http',s)+'$host'+r.url},webSocketURL: function(s){return _s('ws',s)+'$host'+r.url}}}
             |$routeDefs
             |})($name)
          """.stripMargin
    }

  }

}
