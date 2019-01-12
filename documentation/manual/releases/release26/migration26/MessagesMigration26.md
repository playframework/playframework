<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# I18N API Migration

There are a number of changes to the I18N API to make working with messages and languages easier to use, particularly with forms and templates.

## Java API

### Refactored Messages API to interfaces

The `play.i18n` package has changed to make access to [`Messages`](api/java/play/i18n/Messages.html) easier.  These changes should be transparent to the user, but are provided here for teams extending the I18N API.

[`Messages`](api/java/play/i18n/Messages.html) is now an interface, and there is a [`MessagesImpl`](api/java/play/i18n/MessagesImpl.html) class that implements that interface.

### Deprecated / Removed Methods

The static deprecated methods in [`play.i18n.Messages`](api/java/play/i18n/Messages.html) have been removed in 2.6.x, as there are equivalent methods on the [`MessagesApi`](api/java/play/i18n/MessagesApi.html) instance.

## Scala API

### Removed Implicit Default Lang

The [`Lang`](api/scala/play/api/i18n/Lang.html) singleton object has a `defaultLang` that points to the JVM default Locale. Pre 2.6.x, `defaultLang` was an implicit value, with the result that it could be used in implicit scope resolution if no `Lang` was found in local scope.  This setting was too general and resulted in bugs where `defaultLang` was being used instead of a request's locale, if the request was not declared as implicit.

As a result, the implicit has been removed, and so what was:

```scala
object Lang {
  implicit lazy val defaultLang: Lang = Lang(java.util.Locale.getDefault)
}
```

is now:

```scala
object Lang {
  lazy val defaultLang: Lang = Lang(java.util.Locale.getDefault)
}
```

Any code that was relying on this implicit should use `Lang.defaultLang` explicitly.

### Refactored Messages API to traits

 The `play.api.i18n` package has changed to make access to [`Messages`](api/scala/play/api/i18n/Messages.html) instances easier and reduce the number of implicits in play.  These changes should be transparent to the user, but are provided here for teams extending the I18N API.

[`Messages`](api/scala/play/api/i18n/Messages.html) is now a trait (rather than a case class).  The case class is now [`MessagesImpl`](api/scala/play/api/i18n/MessagesImpl.html), which implements [`Messages`](api/scala/play/api/i18n/Messages.html).

### I18nSupport Implicit Conversion

If you are upgrading directly from Play 2.5 to Play 2.6, you should know that `I18nSupport` support has changed in 2.6.x.  In 2.5.x, it was possible through a series of implicits to use a "language default" `Messages` instance if the request was not declared to be in implicit scope:

```scala
  def listWidgets = Action {
    val lang = implicitly[Lang] // Uses Lang.defaultLang
    val messages = implicitly[Messages] // Uses I18nSupport.lang2messages(Lang.defaultLang)
    // implicit parameter messages: Messages in requiresMessages template, but no request!
    val content = views.html.requiresMessages(form)
    Ok(content)
  }
```

The [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) implicit conversion now requires an implicit request or request header in scope in order to correctly determine the preferred locale and language for the request.

This means if you have the following:

```scala
def index = Action {

}
```

You need to change it to:

```scala
def index = Action { implicit request =>

}
```

This will allow i18n support to see the request's locale and provide error messages and validation alerts in the user's language.

### Smoother I18nSupport

Using a form inside a controller is a smoother experience in 2.6.x.   [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) contains a [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) instance, which is exposed by [`AbstractController`](api/scala/play/api/mvc/AbstractController.html).  This means that the [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) trait does not require an explicit `val messagesApi: MessagesApi` declaration, as it did in Play 2.5.x.

```scala
class FormController @Inject()(components: ControllerComponents)
  extends AbstractController(components) with I18nSupport {

  import play.api.data.validation.Constraints._

  val userForm = Form(
    mapping(
      "name" -> text.verifying(nonEmpty),
      "age" -> number.verifying(min(0), max(100))
    )(UserData.apply)(UserData.unapply)
  )

  def index = Action { implicit request =>
    // use request2messages implicit conversion method
    Ok(views.html.user(userForm))
  }

  def showMessage = Action { request =>
    // uses type enrichment
    Ok(request.messages("hello.world"))
  }

  def userPost = Action { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.user(formWithErrors))
      },
      user => {
        Redirect(routes.FormController.index()).flashing("success" -> s"User is ${user}!")
      }
    )
  }
}
```

Note there is now also type enrichment in [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) which adds `request.messages` and `request.lang`.  This can be added either by extending from [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html), or by `import I18nSupport._`.  The import version does not contain the `request2messages` implicit conversion.

## Integrated Messages with MessagesProvider

A new [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) trait is available, which exposes a [`Messages`](api/scala/play/api/i18n/Messages.html) instance.

```scala
trait MessagesProvider {
  def messages: Messages
}
```

[`MessagesImpl`](api/scala/play/api/i18n/MessagesImpl.html) implements [`Messages`](api/scala/play/api/i18n/Messages.html) and [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html), and returns itself by default.

All the template helpers now take [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) as an implicit parameter, rather than a straight `Messages` object, i.e. `inputText.scala.html` takes the following:

```scala
@(field: play.api.data.Field, args: (Symbol,Any)*)(implicit handler: FieldConstructor, messagesProvider: play.api.i18n.MessagesProvider)
```

The benefit to using a [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is that otherwise, if you used implicit `Messages`, you would have to introduce implicit conversions from other types like `Request` in places where those implicits could be confusing.

### MessagesRequest and MessagesAbstractController

To assist, there's [`MessagesRequest`](api/scala/play/api/mvc/MessagesRequest.html), which is a [`WrappedRequest`](api/scala/play/api/mvc/WrappedRequest.html) that implements [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) and provides the preferred language.

You can access a [`MessagesRequest`](api/scala/play/api/mvc/MessagesRequest.html) by using a [`MessagesActionBuilder`](api/scala/play/api/mvc/MessagesActionBuilder.html):

```scala

class MyController @Inject()(
    messagesAction: MessagesActionBuilder,
    cc: ControllerComponents
  ) extends AbstractController(cc) {
    def index = messagesAction { implicit request: MessagesRequest[AnyContent] =>
       Ok(views.html.formTemplate(form)) // twirl template with form builders
    }
}

```

Or you can use [`MessagesAbstractController`](api/scala/play/api/mvc/MessagesAbstractController.html), which swaps out the default `Action` that provides `MessagesRequest` instead of `Request` in the block:

```scala

class MyController @Inject() (
  mcc: MessagesControllerComponents
) extends MessagesAbstractController(mcc) {

  def index = Action { implicit request: MessagesRequest[AnyContent] =>
      Ok(s"The messages are ${request.messages}")
  }
}

```

Here's a complete example using a form with a CSRF action (assuming that you have CSRF filter disabled):

```scala

class MyController @Inject() (
  addToken: CSRFAddToken,
  checkToken: CSRFCheck,
  mcc: MessagesControllerComponents
) extends MessagesAbstractController(mcc) {

  import play.api.data.Form
  import play.api.data.Forms._

  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(UserData.apply)(UserData.unapply)
  )

  def index = addToken {
    Action { implicit request =>
      Ok(views.html.formpage(userForm))
    }
  }

  def userPost = checkToken {
    Action { implicit request =>
      userForm.bindFromRequest.fold(
        formWithErrors => {
          play.api.Logger.info(s"unsuccessful user submission")
          BadRequest(views.html.formpage(formWithErrors))
        },
        user => {
          play.api.Logger.info(s"successful user submission ${user}")
          Redirect(routes.MyController.index()).flashing("success" -> s"User is ${user}!")
        }
      )
    }
  }
}

```

Because `MessagesRequest` is a `MessagesProvider`, you only have to define the request as implicit and it will carry through to the template.  This is especially useful when CSRF checks are involved.  The `formpage.scala.html` page is as follow:

```scala

@(userForm: Form[UserData])(implicit request: MessagesRequestHeader)

@helper.form(action = routes.MyController.userPost()) {
    @views.html.helper.CSRF.formField
    @helper.inputText(userForm("name"))
    @helper.inputText(userForm("age"))
    <input type="submit" value="SUBMIT"/>
}

```

Note that because the body of the `MessageRequest` is not relevant to the template, we can use `MessagesRequestHeader` here instead of `MessageRequest[_]`.

Please see [[passing messages to form helpers|ScalaForms#Passing-MessagesProvider-to-Form-Helpers]] for more details.

### DefaultMessagesApi component

The default implementation of [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) is [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html).  [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html) used to take [`Configuration`](api/scala/play/api/Configuration.html) and [`Environment`](api/scala/play/api/Environment.html) directly, which made it awkward to deal with in forms.  For unit testing purposes, [`DefaultMessagesApi`](api/scala/play/api/i18n/DefaultMessagesApi.html) can be instantiated without arguments, and will take a raw map.

```scala

import play.api.data.Forms._
import play.api.data._
import play.api.i18n._

val messagesApi = new DefaultMessagesApi(
  Map("en" ->
    Map("error.min" -> "minimum!")
  )
)
implicit val request = {
  play.api.test.FakeRequest("POST", "/")
    .withFormUrlEncodedBody("name" -> "Play", "age" -> "-1")
}
implicit val messages = messagesApi.preferred(request)

def errorFunc(badForm: Form[UserData]) = {
  BadRequest(badForm.errorsAsJson)
}

def successFunc(userData: UserData) = {
  Redirect("/").flashing("success" -> "success form!")
}

val result = Future.successful(form.bindFromRequest().fold(errorFunc, successFunc))
Json.parse(contentAsString(result)) must beEqualTo(Json.obj("age" -> Json.arr("minimum!")))

```

For functional tests that involve configuration, the best option is to use `WithApplication` and pull in an injected [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html):

```scala

import play.api.test.{ PlaySpecification, WithApplication }
import play.api.i18n._

class MessagesSpec extends PlaySpecification {

  sequential

  implicit val lang = Lang("en-US")

  "Messages" should {
    "provide default messages" in new WithApplication(_.requireExplicitBindings()) {
      val messagesApi = app.injector.instanceOf[MessagesApi]
      val javaMessagesApi = app.injector.instanceOf[play.i18n.MessagesApi]

      val msg = messagesApi("constraint.email")
      val javaMsg = javaMessagesApi.get(new play.i18n.Lang(lang), "constraint.email")

      msg must ===("Email")
      msg must ===(javaMsg)
    }
    "permit default override" in new WithApplication(_.requireExplicitBindings()) {
      val messagesApi = app.injector.instanceOf[MessagesApi]
      val msg = messagesApi("constraint.required")

      msg must ===("Required!")
    }
  }
}

```

If you need to customize the configuration, it's better to add configuration values into the [`GuiceApplicationBuilder`](api/scala/play/api/inject/guice/GuiceApplicationBuilder.html) rather than use the [`DefaultMessagesApiProvider`](api/scala/play/api/i18n/DefaultMessagesApiProvider.html) directly.

### Deprecated Methods

`play.api.i18n.Messages.Implicits.applicationMessagesApi` and `play.api.i18n.Messages.Implicits.applicationMessages` have been deprecated, because they rely on an implicit `Application` instance.

The `play.api.mvc.Controller.request2lang` method has been deprecated, because it was using a global `Application` under the hood.

The `play.api.i18n.I18nSupport.request2Messages` implicit conversion method has been moved to `I18NSupportLowPriorityImplicits.request2Messages`, and deprecated in favor of `request.messages` type enrichment, which is clearer overall.

The `I18NSupportLowPriorityImplicits.lang2Messages` implicit conversion has been moved out to `LangImplicits.lang2Messages`, because of confusion when both implicit Request and a Lang were in scope. Please extend the [`play.api.i18n.LangImplicits`](api/scala/play/api/i18n/LangImplicits.html) trait specifically if you want to create a `Messages` from an implicit `Lang`.
