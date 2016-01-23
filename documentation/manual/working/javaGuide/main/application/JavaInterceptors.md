<!--- Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com> -->
# Intercepting requests

> **NOTE**: The `GlobalSettings` class is deprecated in 2.5.x.  Please see the [[Removing `GlobalSettings`|GlobalSettings]] page for how to migrate away from GlobalSettings. 

## Overriding onRequest

One important aspect of  the ```GlobalSettings``` class is that it provides a way to intercept requests and execute business logic before a request is dispatched to an action.

For example:

@[global](code/javaguide/application/intercept/Global.java)

It’s also possible to intercept a specific action method. This can be achieved via [[Action composition|JavaActionsComposition]].
