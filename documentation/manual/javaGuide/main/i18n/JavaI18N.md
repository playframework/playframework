# Externalising messages and internationalization

## Specifying languages supported by your application

The specify your application’s languages, you need a valid language code, specified by a valid **ISO Language Code**, optionally followed by a valid **ISO Country Code**. For example, `fr` or `en-US`.

To start, you need to specify the languages that your application supports in its `conf/application.conf` file:

```
application.langs=en,en-US,fr
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
String title = Messages.get(new Lang("fr"), "home.title")
```

> **Note:** If you have a `Request` in the scope, it will provide a default `Lang` value corresponding to the preferred language extracted from the `Accept-Language` header and matching one the application’s supported languages.

## Formatting messages

Messages can be formatted using the `java.text.MessageFormat` library. For example, if you have defined a message like this:

```
files.summary=The disk {1} contains {0} file(s).
```

You can then specify parameters as:

```
Messages.get("files.summary", d.files.length, d.name)
```

## Retrieving supported languages from an HTTP request

You can retrieve a specific HTTP request’s supported languages:

```
public static Result index() {
  return ok(request().acceptLanguages());
}
```

> **Next:** [[The application Global object | JavaGlobal]]