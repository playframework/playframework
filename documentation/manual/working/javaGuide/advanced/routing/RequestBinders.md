<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Custom Routing

Play provides a mechanism to bind types from path or query string parameters. 

## PathBindable

[PathBindable](api/java/play/mvc/PathBindable.html) allows to bind business objects from the URL path; this means we’ll be able to define routes like `/user/3` to call an action such as the following:

### `controller`

@[path](code/javaguide/binder/controllers/BinderApplication.java)

The `user` parameter will automatically be retrieved using the id extracted from the URL path, e.g. with the following route definition:

### `/conf/routes`

@[user](code/javaguide.binder.routes)

Any type `T` that implements [`PathBindable`](api/java/play/mvc/PathBindable.html) can be bound to/from a path parameter.
It defines abstract methods `bind` (build a value from the path) and `unbind` (build a path fragment from a value).

For a class like:

@[declaration](code/javaguide/binder/models/User.java)

A simple example of the binder's use binding the `:id` path parameter:

@[bind](code/javaguide/binder/models/User.java)

In this example `findById` method is invoked to retrieve `User` instance.

> **Note:** in a real application such method should be lightweight and not involve e.g. DB access, because the code is called on the server IO thread and must be totally non-blocking. You would therefore for example use simple objects identifier as path bindable, and retrieve the real values using action composition.

## QueryStringBindable

A similar mechanism is used for query string parameters; a route like `/age` can be defined to call an action such as:

### `controller`

@[query](code/javaguide/binder/controllers/BinderApplication.java)

The `age` parameter will automatically be retrieved using parameters extracted from the query string e.g. `/age?from=1&to=10`

Any type `T` that implements [`QueryStringBindable`](api/java/play/mvc/QueryStringBindable.html) can be bound to/from query one or more query string parameters. Similar to [`PathBindable`](api/java/play/mvc/PathBindable.html), it defines abstract methods `bind` and `unbind`.

For a class like:

@[declaration](code/javaguide/binder/models/AgeRange.java)

A simple example of the binder's use binding the `:from` and `:to` query string parameters:

@[bind](code/javaguide/binder/models/AgeRange.java)
