<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Internationalization with Messages

## Specifying languages supported by your application

You specify languages for your application using language tags, specially formatted strings that identify a specific language. Language tags can specify simple languages, such as "en" for English, a specific regional dialect of a language (such as "en-AU" for English as used in Australia), a language and a script (such as "az-Latn" for Azerbaijani written in Latin script), or a combination of several of these (such as "zh-cmn-Hans-CN" for Chinese, Mandarin, Simplified script, as used in China).

To start you need to specify the languages supported by your application in the `conf/application.conf` file:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

These language tags will be used to create [`play.api.i18n.Lang`](api/scala/play/api/i18n/Lang.html) instances. To access the languages supported by your application, you can inject a [`play.api.i18n.Langs`](api/scala/play/api/i18n/Langs.html) component into your class:

@[inject-langs](code/scalaguide/i18n/ScalaI18nService.scala)

An individual [`play.api.i18n.Lang`](api/scala/play/api/i18n/Lang.html) can be converted to a [`java.util.Locale`](https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html) object by using `lang.toLocale`:

@[lang-to-locale](code/scalaguide/i18n/ScalaI18nService.scala)

## Externalizing messages

You can externalize messages in the `conf/messages.xxx` files.

The default `conf/messages` file matches all languages. Additionally you can specify language-specific message files such as `conf/messages.fr` or `conf/messages.en-US`.

Messages are available through the [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) instance, which can be added via injection.  You can then retrieve messages using the [`play.api.i18n.MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) object:

@[inject-messages-api](code/scalaguide/i18n/ScalaI18nService.scala)

You can also make the language implicit rather than declare it:

@[use-implicit-lang](code/scalaguide/i18n/ScalaI18nService.scala)

## Using Messages and MessagesProvider

Because it's common to want to use messages without having to provide an argument, you can wrap a given `Lang` together with the [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) to create a [`play.api.i18n.Messages`](api/scala/play/api/i18n/MessagesImpl.html) instance.  The [`play.api.i18n.MessagesImpl`](api/scala/play/api/i18n/MessagesImpl.html) case class implements the [`Messages`](api/scala/play/api/i18n/Messages.html) trait if you want to create one directly:

@[using-messages-impl](code/scalaguide/i18n/ScalaI18nService.scala)

You can also use Singleton object methods with an implicit [`play.api.i18n.MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html):

@[using-implicit-messages-provider](code/scalaguide/i18n/ScalaI18nService.scala)

A [`play.api.i18n.MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is a trait that can provide a [`Messages`](api/scala/play/api/i18n/Messages.html) object on demand.  An instance of [`Messages`](api/scala/play/api/i18n/Messages.html) extends [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) and returns itself.
  
[`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is most useful when extended by something that is not a `Messages`:

@[custom-message-provider](code/scalaguide/i18n/ScalaI18nService.scala)

## Using Messages with Controllers

You can add [`Messages`](api/scala/play/api/i18n/Messages.html) support to your request by extending [`MessagesAbstractController`](api/scala/play/api/mvc/MessagesAbstractController.html) or [`MessagesBaseController`](api/scala/play/api/mvc/MessagesBaseController.html):
 
@[i18n-messagescontroller](code/ScalaI18N.scala)
 
Or by adding the [`play.api.i18n.I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) trait to your controller and ensuring an instance of [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) is in scope, which will use implicits to convert a request.

@[i18n-support](code/ScalaI18N.scala)

All the form helpers in Twirl templates take [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html), and it is assumed that a [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is passed into the template as an implicit parameter when processing a form.  

```twirl
@(form: Form[Foo])(implicit messages: MessagesProvider)

@helper.inputText(field = form("name")) @* <- takes MessagesProvider *@
```

### Retrieving supported language from an HTTP request

You can retrieve the languages supported by a specific HTTP request:

@[http-supported-langs](code/scalaguide/i18n/ScalaI18nService.scala)

### Request Types

The [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) trait adds the following methods to a [`Request`](api/scala/play/api/mvc/Request.html):

* `request.messages` returns an instance of `Messages`, using an implicit `MessagesApi` 
* `request.lang` returns the preferred `Lang`, using an implicit `MessagesApi` 

The preferred language is extracted from the `Accept-Language` header (and optionally the language cookie) and matching one of the [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) supported languages using `messagesApi.preferred`.

### Language Cookie Support

The [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) also adds two convenient methods to `Result`:

* `result.withLang(lang: Lang)` is used to set the language using Play's language cookie. 
* `result.clearingLang` is used to clear the language cookie.

For example:

@[lang-cookies](code/scalaguide/i18n/ScalaI18nService.scala)

The `withLang` method sets the cookie named `PLAY_LANG` for future requests, while clearingLang discards the cookie, and Play will choose the language based on the client's Accept-Language header.

The cookie name can be changed by changing the configuration parameter: `play.i18n.langCookieName`.

## Implicit Lang Conversion

The [`LangImplicits`](api/scala/play/api/i18n/LangImplicits.html) trait can be declared on a controller to implicitly convert a request to a `Messages` given an implicit `Lang` instance.

@[using-lang-implicits-trait](code/scalaguide/i18n/ScalaI18nService.scala)

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

## Explicit MessagesApi

The default implementation of [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) is [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html).  You can see [[unit testing|ScalaTestingWithSpecs2#Unit-Testing-Messages]] and [[functional testing|ScalaFunctionalTestingWithSpecs2#Testing-Messages-API]] examples in the testing section of the documentation.

You can also use [`Helpers.stubMessagesApi()`](api/scala/play/api/test/Helpers$.html#stubMessagesApi\(messages:Map[String,Map[String,String]],langs:play.api.i18n.Langs,langCookieName:String,langCookieSecure:Boolean,langCookieHttpOnly:Boolean,httpConfiguration:play.api.http.HttpConfiguration\):play.api.i18n.MessagesApi) in testing to provide a premade empty MessagesApi.