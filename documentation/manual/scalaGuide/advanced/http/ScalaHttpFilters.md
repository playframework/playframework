# Filters

Play provides a simple filter API for applying global filters to each request.

## Filters vs action composition

The filter API is intended for cross cutting concerns that are applied indiscriminately to all routes.  For example, here are some common use cases for filters:

* Logging/metrics collection
* GZIP encoding
* Blanket security filters

In contrast, [[action composition|ScalaActionsComposition]] is intended for route specific concerns, such as authentication and authorisation, caching and so on.  If your filter is not one that you want applied to every route, consider using action composition instead, it is far more powerful.  And don't forget that you can create your own action builders that compose your own custom defined sets of actions to each route, to minimise boilerplate.

## A simple logging filter

The following is a simple filter that times and logs how long a request takes to execute in Play framework:

```scala
import play.api.mvc._

object LoggingFilter extends Filter {
  def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {
    val start = System.currentTimeMillis

    def logTime(result: PlainResult): Result = {
      val time = System.currentTimeMillis - start
      Logger.info(s"${rh.method} ${rh.uri} took ${time}ms and returned ${result.header.status}")
      result.withHeaders("Request-Time" -> time.toString)
    }
    
    next(rh) match {
      case plain: PlainResult => logTime(plain)
      case async: AsyncResult => async.transform(logTime)
    }
  }
}
```

Let's understand what's happening here.  The first thing to notice is the signature of the `apply` method.  It's a curried function, with the first parameter, `next`, being a function that takes a request header and produces a result, and the second parameter, `rh`, being a request header.

The `next` parameter represents the next action in the filter chain.  Invoking it will cause the action to be invoked.  In most cases you will probably want to invoke this at some point in your future.  You may decide to not invoke it if for some reason you want to block the request.

The `rh` parameter is the actual request header for the request.

The next thing in the code is a function that logs the request.  This function takes a `PlainResult`, and after logging the request time, adds a header to the response that records the `Request-Time`, and returns that result.

Finally the next action is invoked, and pattern matched on the result it returns.  A result can either be a `PlainResult` or a `AsyncResult`, an `AsyncResult` is a result that will eventually be redeemed as a `PlainResult`.  In both cases, the `logTime` function needs to be invoked, but is invoked in a slightly different way for each.  Since if it's a `PlainResult` the result is available now, it just invokes `logTime` directly.  However, if it's `AsyncResult` the result is not yet available.  So, the `logTime` function is passed to the `transform` method to be invoked later, when the `PlainResult` is available.

### A simpler syntax

You can use a simpler syntax for declaring a filter if you wish:

```scala
val loggingFilter = Filter { (next, rh) =>
  val start = System.currentTimeMillis

  def logTime(result: PlainResult): Result = {
    val time = System.currentTimeMillis - start
    Logger.info(s"${rh.method} ${rh.uri} took ${time}ms and returned ${result.header.status}")
    result.withHeaders("Request-Time" -> time.toString)
  }
    
  next(rh) match {
    case plain: PlainResult => logTime(plain)
    case async: AsyncResult => async.transform(logTime)
  }
}
```

Since this is a val, this can only be used inside some scope.

## Using filters

The simplest way to use a filter is to extends the `WithFilters` trait on your `Global` object:

```scala
import play.api.mvc._

object Global extends WithFilters(LoggingFilter, new GzipFilter()) {
  ...
}
```

You can also invoke a filter manually:

```scala
import play.api._

object Global extends GlobalSettings {
  override def doFilter(action: EssentialAction) = LoggingFilter(action)
}
```

## Where do filters fit in?

Filters wrap the action after the action has been looked up by the router.  This means you cannot use a filter to transform a path, method or query parameter to impact the router.  However you can direct the request to a different action by invoking that action directly from the filter, though be aware that this will bypass the rest of the filter chain.  If you do need to modify the request before the router is invoked, a better way to do this would be to place your logic in `Global.onRouteRequest` instead.

Since filters are applied after routing is done, it is possible to access routing information from the request, via the `tags` map on the `RequestHeader`.  For example, you might want to log the time against the action method.  In that case, you might update the `logTime` method to look like this:

```scala
  def logTime(result: PlainResult): Result = {
    val time = System.currentTimeMillis - start
    val action = rh.tags(Routes.ROUTE_CONTROLLER) + "." + rh.tags(Routes.ROUTE_ACTION_METHOD)
    Logger.info(s"${action} took ${time}ms and returned ${result.header.status}")
    result.withHeaders("Request-Time" -> time.toString)
  }
```

> Routing tags are a feature of the Play router.  If you use a custom router, or return a custom action in `Glodal.onRouteRequest`, these parameters may not be available.

## More powerful filters

Play provides a lower level filter API called `EssentialFilter` which gives you full access to the body of the request.  This API allows you to wrap [[EssentialAction|HttpApi]] with another action.

Here is the above filter example rewritten as an `EssentialFilter`:

```scala
import play.api.mvc._

object LoggingFilter extends EssentialFilter {
  def apply(next: EssentialAction) = new EssentialAction {
    def apply(rh: RequestHeader) = {
      val start = System.currentTimeMillis

      def logTime(result: PlainResult): Result = {
        val time = System.currentTimeMillis - start
        Logger.info(s"${rh.method} ${rh.uri} took ${time}ms and returned ${result.header.status}")
        result.withHeaders("Request-Time" -> time.toString)
      }
    
      next(rh).map {
        case plain: PlainResult => logTime(plain)
        case async: AsyncResult => async.transform(logTime)
      }
    }
  }
}
```

The key difference here, apart from creating a new `EssentialAction` to wrap the passed in `next` action, is when we invoke next, we get back an `Iteratee`.  You could wrap this in an `Enumeratee` to do some transformations if you wished.  We then `map` the result of the iteratee, and handle it with a partial function, in the same way as in the simple form.

> Although it may seem that there are two different filter APIs, there is only one, `EssentialFilter`.  The simpler `Filter` API in the earlier examples extends `EssentialFilter`, and implements it by creating a new `EssentialAction`.  The passed in callback makes it appear to skip the body parsing by creating a promise for the `Result`, and returning that in an `AsyncResult`, while the body parsing and the rest of the action are executed asynchronously.
