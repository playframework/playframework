<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Protecting against Cross Site Request Forgery

Cross Site Request Forgery (CSRF) is a security exploit where an attacker tricks a victims browser into making a request using the victims session.  Since the session token is sent with every request, if an attacker can coerce the victims browser to make a request on their behalf, the attacker can make requests on the users behalf.

It is recommended that you familiarise yourself with CSRF, what the attack vectors are, and what the attack vectors are not.  We recommend starting with [this information from OWASP](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29).

Simply put, an attacker can coerce a victims browser to make the following types of requests:

* All `GET` requests
* `POST` requests with bodies of type `application/x-www-form-urlencoded`, `multipart/form-data` and `text/plain`

An attacker can not:

* Coerce the browser to use other request methods such as `PUT` and `DELETE`
* Coerce the browser to post other content types, such as `application/json`
* Coerce the browser to send new cookies, other than those that the server has already set
* Coerce the browser to set arbitrary headers, other than the normal headers the browser adds to requests

Since `GET` requests are not meant to be mutative, there is no danger to an application that follows this best practice.  So the only requests that need CSRF protection are `POST` requests with the above mentioned content types.

### Play's CSRF protection

Play supports multiple methods for verifying that a request is not a CSRF request.  The primary mechanism is a CSRF token.  This token gets placed either in the query string or body of every form submitted, and also gets placed in the users session.  Play then verifies that both tokens are present and match.

To allow simple protection for non browser requests, such as requests made through AJAX, Play also supports the following:

* If an `X-Requested-With` header is present, Play will consider the request safe.  `X-Requested-With` is added to requests by many popular Javascript libraries, such as jQuery.
* If a `Csrf-Token` header with value `nocheck` is present, or with a valid CSRF token, Play will consider the request safe.

## Applying a global CSRF filter

Play provides a global CSRF filter that can be applied to all requests.  This is the simplest way to add CSRF protection to an application.  To enable the global filter, add the Play filters helpers dependency to your project in `build.sbt`:

```scala
libraryDependencies += filters
```

Now add them to your `Filters` class as described in [[HTTP filters|ScalaHttpFilters]]:

@[http-filters](code/ScalaCsrf.scala)

### Getting the current token

The current CSRF token can be accessed using the `getToken` method.  It takes an implicit `RequestHeader`, so ensure that one is in scope.

@[get-token](code/ScalaCsrf.scala)

To help in adding CSRF tokens to forms, Play provides some template helpers.  The first one adds it to the query string of the action URL:

@[csrf-call](code/scalaguide/forms/csrf.scala.html)

This might render a form that looks like this:

```html
<form method="POST" action="/items?csrfToken=1234567890abcdef">
   ...
</form>
```

If it is undesirable to have the token in the query string, Play also provides a helper for adding the CSRF token as hidden field in the form:

@[csrf-input](code/scalaguide/forms/csrf.scala.html)

This might render a form that looks like this:

```html
<form method="POST" action="/items">
   <input type="hidden" name="csrfToken" value="1234567890abcdef"/>
   ...
</form>
```

The form helper methods all require an implicit token or request to be available in scope.  This will typically be provided by adding an implicit `RequestHeader` parameter to your template, if it doesn't have one already.

### Adding a CSRF token to the session

To ensure that a CSRF token is available to be rendered in forms, and sent back to the client, the global filter will generate a new token for all GET requests that accept HTML, if a token isn't already available in the incoming request.

## Applying CSRF filtering on a per action basis

Sometimes global CSRF filtering may not be appropriate, for example in situations where an application might want to allow some cross origin form posts.  Some non session based standards, such as OpenID 2.0, require the use of cross site form posting, or use form submission in server to server RPC communications.

In these cases, Play provides two actions that can be composed with your applications actions.

The first action is the `CSRFCheck` action, and it performs the check.  It should be added to all actions that accept session authenticated POST form submissions:

@[csrf-check](code/ScalaCsrf.scala)

The second action is the `CSRFAddToken` action, it generates a CSRF token if not already present on the incoming request.  It should be added to all actions that render forms:

@[csrf-add-token](code/ScalaCsrf.scala)

A more convenient way to apply these actions is to use them in combination with Play's `ActionBuilder`:

@[csrf-action-builder](code/ScalaCsrf.scala)

Then you can minimise the boiler plate code necessary to write actions:

@[csrf-actions](code/ScalaCsrf.scala)

## CSRF configuration options

The full range of CSRF configuration options can be found in the filters [reference.conf](resources/confs/filters-helpers/reference.conf).  Some examples include:

* `play.filters.csrf.token.name` - The name of the token to use both in the session and in the request body/query string. Defaults to `csrfToken`.
* `play.filters.csrf.cookie.name` - If configured, Play will store the CSRF token in a cookie with the given name, instead of in the session.
* `play.filters.csrf.cookie.secure` - If `play.filters.csrf.cookie.name` is set, whether the CSRF cookie should have the secure flag set.  Defaults to the same value as `play.http.session.secure`.
* `play.filters.csrf.body.bufferSize` - In order to read tokens out of the body, Play must first buffer the body and potentially parse it.  This sets the maximum buffer size that will be used to buffer the body.  Defaults to 100k.
* `play.filters.csrf.token.sign` - Whether Play should use signed CSRF tokens.  Signed CSRF tokens ensure that the token value is randomised per request, thus defeating BREACH style attacks.
