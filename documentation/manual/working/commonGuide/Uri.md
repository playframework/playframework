<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Play's Uri Encoding / Decoding

Play uses the following Strategies to decode / encode Uri's.
If your code needs some more exotic Strategies you need to parse Query Strings and Paths by yourself.

## Path

### Decoding

TODO

### Encoding

TODO

## Query Strings

### Decoding

Basically Play will only split Query String's by `?` and `&` and Key-Value-Pairs with `=` we won't split Matrix Param.
In concret, this means that a `?filter=a&filter=b` will be `List("a", "b")` while a `?filter=a,b` or `?filter=a;b` will be a `List("a,b")` or `List("a;b")`.

Here are some more Example's:

- A Query String of `?foo` will yield a `Map(foo -> Nil)`
- A Query String of `?filter=a,b` will yield a `Map(filter -> List("a,b"))`
- A Query String of `?filter=a,b&filter=c` will yield a `Map(filter -> List("a,b", "c"))`
- A Query String of `?=hello` will yield a `Map("" -> List("hello"))`

This follows closely [[RFC3896]](https://tools.ietf.org/html/rfc3986#section-3.4), the spec actually won't say anything about how other (sub) delimiters should be treated so we won't treat them as is and won't split on any of those.

### Encoding

TODO