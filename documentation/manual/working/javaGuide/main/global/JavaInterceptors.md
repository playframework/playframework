<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Intercepting requests

## Overriding onRequest

One important aspect of  the ```GlobalSettings``` class is that it provides a way to intercept requests and execute business logic before a request is dispatched to an action.

For example:

@[global](code/javaguide/global/intercept/Global.java)

It’s also possible to intercept a specific action method. This can be achieved via [[Action composition| JavaActionsComposition]].
