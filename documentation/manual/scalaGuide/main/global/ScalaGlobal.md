# Application global settings

## The Global object

Defining a `Global` object in your project allows you to handle global settings for your application. This object must be defined in the default (empty) package.

```scala
import play.api._

object Global extends GlobalSettings {

}
```

> **Tip:** You can also specify a custom `GlobalSettings` implementation class name using the `application.global` configuration key.

## Hooking into application start and stop events

You can override the `onStart` and `onStop` methods to be notified of the events in the application life-cycle:

```scala
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
  }  
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }  
    
}
```

## Providing an application error page

When an exception occurs in your application, the `onError` operation will be called. The default is to use the internal framework error page:

```scala
import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable) = {
    InternalServerError(
      views.html.errorPage(ex)
    )
  }  
    
}
```

## Handling missing actions and binding errors

If the framework doesn’t find an `Action` for a request, the `onHandlerNotFound` operation will be called:

```scala
import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound(
      views.html.notFoundPage(request.path)
    )
  }  
    
}
```

The `onBadRequest` operation will be called if a route was found, but it was not possible to bind the request parameters:

```scala
import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  override def onBadRequest(request: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }  
    
}
```

> **Next:** [[Intercepting requests | ScalaInterceptors]]