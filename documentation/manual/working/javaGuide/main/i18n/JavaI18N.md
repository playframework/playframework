<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Internationalization with Messages

## Specifying languages supported by your application

You specify languages for your application using language tags, specially formatted strings that identify a specific language. Language tags can specify simple languages, such as "en" for English, a specific regional dialect of a language (such as "en-AU" for English as used in Australia), a language and a script (such as "az-Latn" for Azerbaijani written in Latin script), or a combination of several of these (such as "zh-cmn-Hans-CN" for Chinese, Mandarin, Simplified script, as used in China).

To start you need to specify the languages supported by your application in the `conf/application.conf` file:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

These language tags will be used to create [`play.i18n.Lang`](api/java/play/i18n/Lang.html) instances. To access the languages supported by your application, you can inject a [`play.i18n.Langs`](api/java/play/i18n/Langs.html) component into your class:

@[inject-lang](code/javaguide/i18n/MyService.java)

An individual [`play.i18n.Lang`](api/java/play/i18n/Lang.html) can be converted to a [`java.util.Locale`](https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html) object by using `lang.toLocale()` method:

@[lang-to-locale](code/javaguide/i18n/MyService.java)

## Externalizing messages

You can externalize messages in the `conf/messages.xxx` files.

The default `conf/messages` file matches all languages. You can specify additional language messages files, such as `conf/messages.fr` or `conf/messages.en-US`.

Messages are available through the [`MessagesApi`](api/java/play/i18n/MessagesApi.html) instance, which can be added via injection.  You can then retrieve messages using the [`play.i18n.Messages`](api/java/play/i18n/Messages.html) object:

@[current-lang-render](code/javaguide/i18n/MyService.java)

If you don't want to use `preferred(...)` to retrieve a `Messages` object you can directly get a message string by specifying a message's language explicitly:

@[specify-lang-render](code/javaguide/i18n/JavaI18N.java)

Note that you should inject the [`play.i18n.MessagesApi`](api/java/play/i18n/MessagesApi.html) class, using [[dependency injection|JavaDependencyInjection]].  For example, using Guice you would do the following:

@[inject-messages-api](code/javaguide/i18n/MyService.java)

## Use in Controllers

If you are in a Controller, you can get the `Messages` instance through the current `Http.Request`:

@[show-request-messages](code/javaguide/i18n/JavaI18N.java)

`MessagesApi.preferred(request)` determines the language by:

1. Seeing if the `Request` has a transient lang set by checking its `transientLang()` method.
2. Looking for a `PLAY_LANG` cookie in the request.
3. Looking at the `Accept-Language` headers of the request.
4. Using the application's default language.

To use `Messages` as part of form processing, please see [[Handling form submission|JavaForms]].

## Use in templates

Once you have the Messages object, you can pass it into the template:

@[template](code/javaguide/i18n/hellotemplate.scala.html)

There is also a shorter form that's equivalent to `messages.at` which many people find useful.

@[template](code/javaguide/i18n/hellotemplateshort.scala.html)

Localized templates that use `messages.at(...)` or simply `messages(...)` are invoked like normal:

@[default-lang-render](code/javaguide/i18n/JavaI18N.java)

## Changing the language

If you want to change the language of the current request (but not for future requests) use `Request.withTransientLang(lang)`, which sets the transient lang of the current request.
Like explained [above](#Use-in-Controllers), the transient language of the request will be taken into account when calling `MessagesApi.preferred(request)`. This is useful to change the language of templates.

@[set-transient-lang-render](code/javaguide/i18n/JavaI18N.java)

If you want to permanently change the language you can do so by calling `withLang` on the `Result`. This will set a `PLAY_LANG` cookie for future requests and will therefore be used when calling `MessagesApi.preferred(request)` in a subsequent request (like shown [above](#Use-in-Controllers)).

@[change-lang-render](code/javaguide/i18n/JavaI18N.java)

## Formatting messages

Messages are formatted using the `java.text.MessageFormat` library. For example, if you have defined a message like this:

```
files.summary=The disk {1} contains {0} file(s).
```

You can then specify parameters as:

```
Messages.get("files.summary", d.files.length, d.name)
```

## Notes on apostrophes

Since Messages uses `java.text.MessageFormat`, please be aware that single quotes are used as a meta-character for escaping parameter substitutions.

For example, if you have the following messages defined:

@[single-apostrophe](code/javaguide/i18n/messages)
@[parameter-escaping](code/javaguide/i18n/messages)

you should expect the following results:

@[single-apostrophe](code/javaguide/i18n/JavaI18N.java)
@[parameter-escaping](code/javaguide/i18n/JavaI18N.java)

## Retrieving supported languages from an HTTP request

You can retrieve a specific HTTP request’s supported languages:

@[accepted-languages](code/javaguide/i18n/JavaI18N.java)

## Using explicit MessagesApi

The default implementation of [`MessagesApi`](api/java/play/i18n/MessagesApi.html) is backed by a [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html) instance which is a Scala API.  But you can instantiate a  [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html) and manually inject it into the `MessagesApi` like:

@[explicit-messages-api](code/javaguide/i18n/JavaI18N.java)

If you need a [`MessagesApi`](api/java/play/i18n/MessagesApi.html) instance for unit testing, you can also use [`play.test.Helpers.stubMessagesApi()`](api/java/play/test/Helpers.html#stubMessagesApi-java.util.Map-play.i18n.Langs-).  See [[Testing your application|JavaTest]] for more details.