<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Externalising messages and internationalization

## Specifying languages supported by your application

You specify languages for your application using language tags, specially formatted strings that identify a specific language. Language tags can specify simple languages, such as "en" for English, a specific regional dialect of a language (such as "en-AU" for English as used in Australia), a language and a script (such as "az-Latn" for Azerbaijani written in Latin script), or a combination of several of these (such as "zh-cmn-Hans-CN" for Chinese, Mandarin, Simplified script, as used in China).

To start you need to specify the languages supported by your application in the `conf/application.conf` file:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

These language tags will be validated used to create `Lang` instances. To access the languages supported by your application, you can inject a `Langs` instance into your component.

## Externalizing messages

You can externalize messages in the `conf/messages.xxx` files.

The default `conf/messages` file matches all languages. You can specify additional language messages files, such as `conf/messages.fr` or `conf/messages.en-US`.

You can retrieve messages for the _current language_ using the `play.i18n.Messages` object:

@[current-lang-render](code/javaguide/i18n/JavaI18N.java)

The _current language_ is found by looking at the `lang` field in the current [`Context`](api/java/play/mvc/Http.Context.html). If there's no current `Context` then the default language is used. The `Context`'s `lang` value is determined by:

1. Seeing if the `Context`'s `lang` field has been set explicitly.
2. Looking for a `PLAY_LANG` cookie in the request.
3. Looking at the `Accept-Language` headers of the request.
4. Using the application's default language.

You can change the `Context`'s `lang` field by calling `changeLang` or `setTransientLang`. The `changeLang` method will change the field and also set a `PLAY_LANG` cookie for future requests. The `setTransientLang` will set the field for the current request, but doesn't set a cookie. See [below](#Use-in-templates) for example usage.

If you don't want to use the current language you can specify a message's language explicitly:

@[specify-lang-render](code/javaguide/i18n/JavaI18N.java)

## Use in templates

You can use the `Messages.get` method from within a template. This will localize a message with the current language.

@[template](code/javaguide/i18n/hellotemplate.scala.html)

You can also use the Scala `Messages` object from within templates. The Scala `Messages` object has a shorter form that's equivalent to `Messages.get` which many people find useful. If you use the Scala `Messages` object remember not to import the Java `play.i18n.Messages` class or they will conflict!

@[template](code/javaguide/i18n/helloscalatemplate.scala.html)

Localized templates that use `Messages.get` or the Scala `Messages` object are invoked like normal:

@[default-lang-render](code/javaguide/i18n/JavaI18N.java)

If you want to change the language for the template you can call `changeLang` on the current [`Context`](api/java/play/mvc/Http.Context.html). This will change the language for the current request, and set the language into a cookie so that the language is changed for future requests:

@[change-lang-render](code/javaguide/i18n/JavaI18N.java)

If you just want to change the language, but only for the current request and not for future requests, call `setTransientLang`:

@[set-transient-lang-render](code/javaguide/i18n/JavaI18N.java)

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

```
public static Result index() {
  return ok(request().acceptLanguages());
}
```
