<!--- Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com> -->
# Externalising messages and internationalization

## Specifying languages supported by your application

To specify your application’s languages, you need a valid language code, specified by a valid **ISO Language Code**, optionally followed by a valid **ISO Country Code**. For example, `fr` or `en-US`.

To start, you need to specify the languages that your application supports in its `conf/application.conf` file:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

## Externalizing messages

You can externalize messages in the `conf/messages.xxx` files. 

The default `conf/messages` file matches all languages. You can specify additional language messages files, such as `conf/messages.fr` or `conf/messages.en-US`.

You can retrieve messages for the current language using the `play.i18n.Messages` object:

```
String title = Messages.get("home.title")
```

You can also specify the language explicitly:

```
String title = Messages.get(new Lang(Lang.forCode("fr")), "home.title")
```

> **Note:** If you have a `Request` in the scope, it will use the preferred language extracted from the `Accept-Language` header and matching one of the application’s supported languages.

## Use in templates
```
@import play.i18n._
@Messages.get("key")
```
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
