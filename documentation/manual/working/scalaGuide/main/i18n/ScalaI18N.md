<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Messages and internationalization

## Specifying languages supported by your application

A valid language code is specified by a valid **ISO 639-2 language code**, optionally followed by a valid **ISO 3166-1 alpha-2 country code**, such as `fr` or `en-US`.

To start you need to specify the languages supported by your application in the `conf/application.conf` file:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

## Externalizing messages

You can externalize messages in the `conf/messages.xxx` files.

The default `conf/messages` file matches all languages. Additionally you can specify language-specific message files such as `conf/messages.fr` or `conf/messages.en-US`.

You can then retrieve messages using the `play.api.i18n.Messages` object:

```scala
val title = Messages("home.title")
```

All internationalization API calls take an implicit `play.api.i18n.Messages` argument retrieved from the current scope. This implicit value contains both the language to use and (essentially) the internationalized messages.

The simplest way to get such an implicit value is to use the `I18nSupport` trait. For instance you can use it as follows in your controllers:

@[i18n-support](code/ScalaI18N.scala)

The `I18nSupport` trait gives you an implicit `Messages` value as long as there is a `Lang` or a `RequestHeader` in the implicit scope.

> **Note:** If you have a `RequestHeader` in the implicit scope, it will use the preferred language extracted from the `Accept-Language` header and matching one of the `MessagesApi` supported languages. You should add a `Messages` implicit parameter to your template like this: `@()(implicit messages: Messages)`.

> **Note:** Also, Play “knows” out of the box how to inject a `MessagesApi` value (that uses the `DefaultMessagesApi` implementation), so you can just annotate your controller with the `@javax.inject.Inject` annotation and let Play automatically wire the components for you.

## Messages format

Messages are formatted using the `java.text.MessageFormat` library. For example, assuming you have message defined like:

```
files.summary=The disk {1} contains {0} file(s).
```

You can then specify parameters as:

```scala
Messages("files.summary", d.files.length, d.name)
```

## Notes on apostrophes

Since Messages uses `java.text.MessageFormat`, please be aware that single quotes are used as a meta-character for escaping parameter substitutions.

For example, if you have the following messages defined:

@[apostrophe-messages](code/scalaguide/i18n/messages)
@[parameter-escaping](code/scalaguide/i18n/messages)

you should expect the following results:

@[apostrophe-messages](code/ScalaI18N.scala)
@[parameter-escaping](code/ScalaI18N.scala)

## Retrieving supported language from an HTTP request

You can retrieve the languages supported by a specific HTTP request:

```scala
def index = Action { request =>
  Ok("Languages: " + request.acceptLanguages.map(_.code).mkString(", "))
}
```
