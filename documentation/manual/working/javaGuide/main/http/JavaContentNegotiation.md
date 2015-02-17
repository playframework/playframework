<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Content negotiation

Content negotiation is a mechanism that makes it possible to serve different representation of a same resource (URI). It is useful *e.g.* for writing Web Services supporting several output formats (XML, JSON, etc.). Server-driven negotiation is essentially performed using the `Accept*` requests headers. You can find more information on content negotiation in the [HTTP specification](http://www.w3.org/Protocols/rfc2616/rfc2616-sec12.html).

## Language

You can get the list of acceptable languages for a request using the `play.mvc.Http.RequestHeader#acceptLanguages` method that retrieves them from the `Accept-Language` header and sorts them according to their quality value. Play uses it to set the `lang` value of request’s HTTP context, so they automatically use the best possible language (if supported by your application, otherwise your application’s default language is used).

## Content

Similarly, the `play.mvc.Http.RequestHeader#acceptedTypes` method gives the list of acceptable result’s MIME types for a request. It retrieves them from the `Accept` request header and sorts them according to their quality factor.

You can test if a given MIME type is acceptable for the current request using the `play.mvc.Http.RequestHeader#accepts` method:

@[negotiate-content](code/javaguide/http/JavaContentNegotiation.java)
