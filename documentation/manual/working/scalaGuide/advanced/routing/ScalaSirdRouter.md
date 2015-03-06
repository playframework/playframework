<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# String Interpolating Routing DSL

Play provides a DSL for defining embedded routers called the *String Interpolating Routing DSL*, or sird for short.  This DSL has many uses, including embedding a light weight Play server, providing custom or more advanced routing capabilities to a regular Play application, and mocking REST services for testing.

Sird is based on a string interpolated extractor object.  Just as Scala supports interpolating parameters into strings for building strings (and any object for that matter), such as `s"Hello $to"`, the same mechanism can also be used to extract parameters out of strings, for example in case statements.

The DSL lives in the [`play.api.routing.sird`](api/scala/index.html#play.api.routing.sird.package) package. Typically, you will want to import this package, as well as a few other packages:

@[imports](code/ScalaSirdRouter.scala)

A simple example of its use is:

@[simple](code/ScalaSirdRouter.scala)

In this case, the `$to` parameter in the interpolated path pattern will extract a single path segment for use in the action.  The `GET` extractor extracts requests with the `GET` method.  It takes a `RequestHeader` and extracts the same `RequestHeader` parameter, it's only used as a convenient filter.  Other method extractors, including `POST`, `PUT` and `DELETE` are also supported.

Like Play's compiled router, sird supports matching multi path segment parameters, this is done by postfixing the parameter with `*`:

@[full-path](code/ScalaSirdRouter.scala)

Regular expressions are also supported, by postfixing the parameter with a regular expression in angled brackets:

@[regexp](code/ScalaSirdRouter.scala)

Query parameters can also be extracted, using the `?` operator to do further extractions on the request, and using the `q` extractor:

@[required](code/ScalaSirdRouter.scala)

While `q` extracts a required query parameter as a `String`, `q_?` or `q_o` if using Scala 2.10 extracts an optional query parameter as `Option[String]`:

@[optional](code/ScalaSirdRouter.scala)

Likewise, `q_*` or `q_s` can be used to extract a sequence of multi valued query parameters:

@[many](code/ScalaSirdRouter.scala)

Multiple query parameters can be extracted using the `&` operator:

@[multiple](code/ScalaSirdRouter.scala)

Since sird is just a regular extractor object (built by string interpolation), it can be combined with any other extractor object, including extracting its sub parameters even further.  Sird provides some useful extractors for some of the most common types out of the box, namely `int`, `long`, `float`, `double` and `bool`:

@[int](code/ScalaSirdRouter.scala)

In the above, `id` is of type `Int`.  If the `int` extractor failed to match, then of course, the whole pattern will fail to match.

Similarly, the same extractors can be used with query string parameters, including multi value and optional query parameters.  In the case of optional or multi value query parameters, the match will fail if any of the values present can't be bound to the type, but no parameters present doesn't cause the match to fail:

@[query-int](code/ScalaSirdRouter.scala)

To further the point that these are just regular extractor objects, you can see here that you can use all other features of a `case` statement, including `@` syntax and if statements:

@[complex](code/ScalaSirdRouter.scala)

