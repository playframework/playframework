<!--- Copyright (C) Lightbend Inc. <https://www.lightbend.com> -->

# OpenID Support in Play

OpenID is a protocol for users to access several services with a single account. As a web developer, you can use OpenID to offer users a way to log in using an account they already have, such as their [Google account](https://developers.google.com/accounts/docs/OpenID). In the enterprise, you may be able to use OpenID to connect to a company’s SSO server.

## The OpenID flow in a nutshell

1. The user gives you his OpenID (a URL).
2. Your server inspects the content behind the URL to produce a URL where you need to redirect the user.
3. The user confirms the authorization on his OpenID provider, and gets redirected back to your server.
4. Your server receives information from that redirect, and checks with the provider that the information is correct.

Step 1 may be omitted if all your users are using the same OpenID provider (for example if you decide to rely completely on Google accounts).

## Usage

To use OpenID, first add `openId`  to your `build.sbt` file:

```scala
libraryDependencies ++= Seq(
  openId
)
```

Now any controller or component that wants to use OpenID will have to declare a dependency on the [OpenIdClient](api/scala/play/api/libs/openid/OpenIdClient.html):

@[dependency](code/ScalaOpenIdSpec.scala)

We've called the `OpenIdClient` instance `openIdClient`, all the following examples will assume this name.

## OpenID in Play

The OpenID API has two important functions:

* `OpenIdClient.redirectURL` calculates the URL where you should redirect the user. It involves fetching the user's OpenID page asynchronously, this is why it returns a `Future[String]`. If the OpenID is invalid, the returned `Future` will fail.
* `OpenIdClient.verifiedId` needs a `RequestHeader` and inspects it to establish the user information, including his verified OpenID. It will do a call to the OpenID server asynchronously to check the authenticity of the information, returning a future of [UserInfo](api/scala/play/api/libs/openid/UserInfo.html). If the information is not correct or if the server check is false (for example if the redirect URL has been forged), the returned `Future` will fail.

If the `Future` fails, you can define a fallback, which redirects back the user to the login page or return a `BadRequest`.

Here is an example of usage (from a controller):

@[flow](code/ScalaOpenIdSpec.scala)

## Extended Attributes

The OpenID of a user gives you his identity. The protocol also supports getting [extended attributes](https://openid.net/specs/openid-attribute-exchange-1_0.html) such as the e-mail address, the first name, or the last name.

You may request *optional* attributes and/or *required* attributes from the OpenID server. Asking for required attributes means the user cannot login to your service if he doesn’t provides them.

Extended attributes are requested in the redirect URL:

@[extended](code/ScalaOpenIdSpec.scala)

Attributes will then be available in the `UserInfo` provided by the OpenID server.
