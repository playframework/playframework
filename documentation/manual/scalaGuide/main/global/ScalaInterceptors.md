# Intercepting requests

## Using Filters

The filter component allows you to intercept requests coming to the application, transform request and responses. Filters provide a nice ability to implement cross-cutting concerns of your application. You can create a filter by extending the `Filter` trait and then add the filter to `Global` object. The following example creates an access log filter that logs the result of all the actions:

```scala
import play.api.mvc._

object Global extends WithFilters(AccessLog)

object AccessLog extends Filter {
  override def apply(next: RequestHeader => Result)(request: RequestHeader): Result = {
    val result = next(request)
    play.Logger.info(request + "\n\t => " + result)
    result
  }
}    
```
The `Global` object extends the `WithFilters` class that allows you to pass one more filters to form a filter chain.

> **Note** `WithFilters` now extends the `GlobalSettings` trait

Here is another example where filter is very useful, check authorization before invoking certain actions:

```scala
object Global extends WithFilters(AuthorizedFilter("editProfile", "buy", "sell")) with GlobalSettings {}
 
object AuthorizedFilter {
  def apply(actionNames: String*) = new AuthorizedFilter(actionNames)
}
 
class AuthorizedFilter(actionNames: Seq[String]) extends Filter {
  override def apply(next: RequestHeader => Result)(request: RequestHeader): Result = {
    if(authorizationRequired(request)) { 
      /* do the auth stuff here */ 
      println("auth required")
      next(request) 
    }
    else next(request)
  }
 
  private def authorizationRequired(request: RequestHeader) = {
    val actionInvoked: String = request.tags.getOrElse(play.api.Routes.ROUTE_ACTION_METHOD, "") 
    actionNames.contains(actionInvoked)
  }
}
```

> **Tip** `RequestHeader.tags` provides lots of useful information about the route used to invoke the action. 

## Overriding onRouteRequest

One another important aspect of  the ```Global``` object is that it provides a way to intercept requests and execute business logic before a request is dispatched to an Action. 

> **Tip** This hook can be also used for hijacking requests, allowing developers to plug-in their own request routing mechanism. 

Let’s see how this works in practice:

```scala
import play.api._
import play.api.mvc._

// Note: this is in the default package.
object Global extends GlobalSettings {

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
     println("executed before every request:" + request.toString)
     super.onRouteRequest(request)
  }

}
```

It’s also possible to intercept a specific Action method, using [[Action composition | ScalaActionsComposition]].


> **Next:** [[Testing your application | ScalaTest]]