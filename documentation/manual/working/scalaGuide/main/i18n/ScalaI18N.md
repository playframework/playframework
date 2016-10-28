<!--- Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com> -->
# Internationalization with Messages

## Specifying languages supported by your application

You specify languages for your application using language tags, specially formatted strings that identify a specific language. Language tags can specify simple languages, such as "en" for English, a specific regional dialect of a language (such as "en-AU" for English as used in Australia), a language and a script (such as "az-Latn" for Azerbaijani written in Latin script), or a combination of several of these (such as "zh-cmn-Hans-CN" for Chinese, Mandarin, Simplified script, as used in China).

To start you need to specify the languages supported by your application in the `conf/application.conf` file:

```
play.i18n.langs = [ "en", "en-US", "fr" ]
```

These language tags will be validated used to create [`play.api.i18n.Lang`](api/scala/play/api/i18n/Lang.html) instances. To access the languages supported by your application, you can inject a [`play.api.i18n.Langs`](api/scala/play/api/i18n/Langs.html) component into your class:

```scala
class MyService @Inject()(langs: Langs) {
  val availableLangs = langs.available
}
```

An individual [`play.api.i18n.Lang`](api/scala/play/api/i18n/Lang.html) can be converted to a [`java.util.Locale`](https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html) object by using `lang.toLocale`:

```scala
val locale: java.util.Locale = lang.toLocale
```

## Externalizing messages

You can externalize messages in the `conf/messages.xxx` files.

The default `conf/messages` file matches all languages. Additionally you can specify language-specific message files such as `conf/messages.fr` or `conf/messages.en-US`.

Messages are available through the [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) instance, which can be added via injection.  You can then retrieve messages using the [`play.api.i18n.MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) object:

```scala
class MyService @Inject()(langs: Langs, messagesApi: MessagesApi) {
  val lang: Lang = langs.availables.head

  val title = messagesApi("home.title")(lang)
}
```

You can also make the language implicit rather than declare it:

```scala
class MyService @Inject()(langs: Langs, messagesApi: MessagesApi) {
  implicit val lang: Lang = langs.availables.head

  val title = messagesApi("home.title")
}
```

## Using Messages and MessagesProvider

Because it's common to want to use messages without having to provide an argument, you can wrap a given `Lang` together with the [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) to create a [`play.api.i18n.Messages`](api/scala/play/api/i18n/MessagesImpl.html) instance.  The [`play.api.i18n.MessagesImpl`](api/scala/play/api/i18n/MessagesImpl.html) case class implements the [`Messages`](api/scala/play/api/i18n/Messages.html) trait if you want to create one directly:

```scala
val messages: Messages = MessagesImpl(lang, messagesApi)
val title = messages("home.title")
```

You can also use Singleton object methods with an implicit [`play.api.i18n.MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html):

```scala
implicit val messagesProvider: MessagesProvider = {
  MessagesImpl(lang, messagesApi)
}
 // uses implicit messages
val title = Messages("home.title")
```

A [`play.api.i18n.MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is a trait that can provide a [`Messages`](api/scala/play/api/i18n/Messages.html) object on demand.  An instance of [`Messages`](api/scala/play/api/i18n/Messages.html) extends [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) and returns itself.
  
[`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is most useful when extended by something that is not a `Messages`:

```
implicit val messagesProvider: MessagesProvider = new MessagesProvider {
  // resolve messages at runtime
  def messages = {
    ...  
  }
}
 // uses implicit messages
val title = Messages("home.title")
```

> **Note:** An example of a [`WrappedRequest`](api/scala/play/api/mvc/WrappedRequest.html) that extends [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is covered in [[passing messages to form helpers|ScalaForms#passing-messages-to-form-helpers]] in the [[ScalaForms]] page.

## Using Messages with Controllers

You can add an implicit [`Messages`](api/scala/play/api/i18n/Messages.html) to your actions by adding the [`play.api.i18n.I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) trait to your controller.  The [`play.api.i18n.I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) trait gives you an implicit `Messages` value as long as there is a `RequestHeader` in the implicit scope.  

If you extend [`Controller`](api/scala/play/api/mvc/Controller.html) directly, this will require that you add `val messagesApi: MessagesApi` to your controller as [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) depends on it.  If you extend [`AbstractController`](api/scala/play/api/mvc/AbstractController.html), then `val messagesApi: MessagesApi` is already provided under the hood and all you have to do is extend [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html):

@[i18n-support](code/ScalaI18N.scala)

The [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) is useful (if not essential) because all the form helpers in Twirl templates take [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html), and it is assumed that a [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is passed into the template as an implicit parameter when processing a form.  

```twirl
@(form: Form[Foo])(implicit messages: MessagesProvider)

@helper.inputText(field = form("name")) @* <- takes MessagesProvider *@
```

> **Note:** This is not a complete guide to form helpers. Please see [[showing forms in a view template|ScalaForms#showing-forms-in-a-view-template]] in the [[ScalaForms]] page for more detailed examples.

### Request Types

[`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) also adds the following methods to a [`Request`](api/scala/play/api/mvc/Request.html):

* `request.messages` returns an instance of `Messages`, using an implicit `MessagesApi` 
* `request.lang` returns the preferred `Lang`, using an implicit `MessagesApi` 

The preferred language is extracted from the `Accept-Language` header (and optionally the language cookie) and matching one of the `MessagesApi` supported languages using `messagesApi.preferred`.

### Language Cookie Support

The [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) also adds two convenient methods to `Result`:

* `result.withLang(lang: Lang)` is used to set the language using Play's language cookie. 
* `result.clearingLang` is used to clear the language cookie.

For example:

```scala
  def homePageInFrench = Action {
    Redirect("/user/home").withLang("fr")
  }
  
  def homePageWithDefaultLang = Action {
    Redirect("/user/home").clearingLang
  }
}  
```

The `withLang` method sets the cookie named `PLAY_LANG` for future requests, while clearingLang discards the cookie, and Play will choose the language based on the client's Accept-Language header.

The cookie name can be changed by changing the configuration parameter: `play.i18n.langCookieName`.

## Implicit Lang Conversion

The [`LangImplicits`](api/scala/play/api/i18n/LangImplicits.html) trait can be declared on a controller to implicitly convert a request to a `Messages` given an implicit `Lang` instance.

```scala
class MyClass @Inject()(val messagesApi: MessagesApi) extends LangImplicits {
  def convertToMessage: Unit = {
    implicit val lang = Lang("en")
    val messages: Messages = lang // implicit conversion
  }
}
```

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
