# Intercepting requests

## Using Filters

The filter component allows you to intercept requests coming to the application, transform request and responses. Filters provide a nice ability to implement cross-cutting concerns of your application. You can create a filter by extending the `Filter` trait and then add the filter to `Global` object. The following example creates an access log filter that logs the result of all the actions:

@[filter-log](code/ScalaInterceptors.scala)


The `Global` object extends the `WithFilters` class that allows you to pass one more filters to form a filter chain.

> **Note** `WithFilters` now extends the `GlobalSettings` trait

Here is another example where filter is very useful, check authorization before invoking certain actions:

@[filter-authorize](code/ScalaInterceptors.scala)


> **Tip** `RequestHeader.tags` provides lots of useful information about the route used to invoke the action. 

## Overriding onRouteRequest

One another important aspect of  the ```Global``` object is that it provides a way to intercept requests and execute business logic before a request is dispatched to an Action. 

> **Tip** This hook can be also used for hijacking requests, allowing developers to plug-in their own request routing mechanism. 

Let’s see how this works in practice:

@[onroute-request](code/ScalaInterceptors.scala)


It’s also possible to intercept a specific Action method, using [[Action composition | ScalaActionsComposition]].


> **Next:** [[Testing your application | ScalaTestingYourApplication]]
