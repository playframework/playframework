<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Content negotiation

Content negotiation is a mechanism that makes it possible to serve different representation of a same resource (URI). It is useful *e.g.* for writing Web Services supporting several output formats (XML, JSON, etc.). Server-driven negotiation is essentially performed using the `Accept*` requests headers. You can find more information on content negotiation in the [HTTP specification](http://www.w3.org/Protocols/rfc2616/rfc2616-sec12.html).

# Language

You can get the list of acceptable languages for a request using the `play.api.mvc.RequestHeader#acceptLanguages` method that retrieves them from the `Accept-Language` header and sorts them according to their quality value. Play uses it in the `play.api.mvc.Controller#lang` method that provides an implicit `play.api.i18n.Lang` value to your actions, so they automatically use the best possible language (if supported by your application, otherwise your application’s default language is used).

# Content

Similarly, the `play.api.mvc.RequestHeader#acceptedTypes` method gives the list of acceptable result’s MIME types for a request. It retrieves them from the `Accept` request header and sorts them according to their quality factor.

Actually, the `Accept` header does not really contain MIME types but media ranges (*e.g.* a request accepting all text results may set the `text/*` range, and the `*/*` range means that all result types are acceptable). Controllers provide a higher-level `render` method to help you to handle media ranges. Consider for example the following action definition:

@[negotiate_accept_type](code/ScalaContentNegotiation.scala)

`Accepts.Html()` and `Accepts.Json()` are extractors testing if a given media range matches `text/html` and `application/json`, respectively. The `render` method takes a partial function from `play.api.http.MediaRange` to `play.api.mvc.Result` and tries to apply it to each media range found in the request `Accept` header, in order of preference. If none of the acceptable media ranges is supported by your function, the `NotAcceptable` result is returned.

For example, if a client makes a request with the following value for the `Accept` header: `*/*;q=0.5,application/json`, meaning that it accepts any result type but prefers JSON, the above code will return the JSON representation. If another client makes a request with the following value for the `Accept` header: `application/xml`, meaning that it only accepts XML, the above code will return `NotAcceptable`.

# Request extractors

See the API documentation of the `play.api.mvc.AcceptExtractors.Accepts` object for the list of the MIME types supported by Play out of the box in the `render` method. You can easily create your own extractor for a given MIME type using the `play.api.mvc.Accepting` case class, for example the following code creates an extractor checking that a media range matches the `audio/mp3` MIME type:

@[extract_custom_accept_type](code/ScalaContentNegotiation.scala)

