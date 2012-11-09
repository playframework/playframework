# Intercepting requests

## Overriding onRouteRequest

One another important aspect of  the ```Global``` object is that it provides a way to intercept requests and execute business logic before a request is dispatched to an Action. 

> **Tip** This hook can be also used for hijacking requests, allowing developers to plug-in their own request routing mechanism. 

Let’s see how this works in practice:

```scala
import play.api._

// Note: this is in the default package.
object Global extends GlobalSettings {

  def onRouteRequest(request: RequestHeader): Option[Handler] = {
     println("executed before every request:" + request.toString)
     super.onRouteRequest(request)
  }

}
```

It’s also possible to intercept a specific Action method, using [[Action composition | ScalaActionsComposition]].


> **Next:** [[Testing your application | ScalaTest]]