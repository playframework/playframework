<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Custom Routing

Play provides a mechanism to bind types from path or query string parameters. 

## PathBindable

PathBindable allows to bind business objects from the URL path; this means we’ll be able to define routes like `/user/3` to call an action such as the following:

- `controller`

@[path](code/scalaguide/binder/controllers/BinderApplication.scala)

The `user` parameter will automatically be retrieved using the id extracted from the URL path, e.g. with the following route definition:

- `/conf/routes`

@[user](code/scalaguide.binder.routes)

You can provide an implementation of [`PathBindable[A]`](api/scala/play/api/mvc/PathBindable.html) for any type A you want to be able to bind directly from the request path. It defines abstract methods `bind` (build a value from the path) and `unbind` (build a path fragment from a value).

For a class definition:

@[declaration](code/scalaguide/binder/models/User.scala)

A simple example of the binder's use binding the `:id` path parameter:

@[bind](code/scalaguide/binder/models/User.scala)


In this example findById method is invoked to retrieve `User` instance; note that in real world such method should be lightweight and not involve e.g. DB access, because the code is called on the server IO thread and must be totally non-blocking.

You would therefore for example use simple objects identifier as path bindable, and retrieve the real values using action composition.

## QueryStringBindable

A similar mechanism is used for query string parameters; a route like `/age` can be defined to call an action such as:

- `controller`

@[query](code/scalaguide/binder/controllers/BinderApplication.scala)

The `age` parameter will automatically be retrieved using parameters extracted from the query string e.g. `/age?from=1&to=10`

You can provide an implementation of [`QueryStringBindable[A]`](api/scala/play/api/mvc/QueryStringBindable.html) for any type A you want to be able to bind directly from the request query string. Similar to [`PathBindable`](api/scala/play/api/mvc/PathBindable.html), it defines abstract methods `bind` and `unbind`.

For a class definition:

@[declaration](code/scalaguide/binder/models/AgeRange.scala)

A simple example of the binder's use binding the `:from` and `:to` query string parameters:

@[bind](code/scalaguide/binder/models/AgeRange.scala)

