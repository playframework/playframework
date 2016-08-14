# Messages API Migration

There are a number of changes to the I18N API to make working with messages and languages easier to use, particularly with forms and templates.

## Java API

### Refactored Messages API to interfaces

The `play.i18n` package has changed to make access to [`Messages`](api/java/play/i18n/Messages.html) easier.  These changes should be transparent to the user, but are provided here for teams extending the I18N API.

[`Messages`](api/java/play/i18n/Messages.html) is now an interface, and there is a [`MessagesImpl`](api/java/play/i18n/MessagesImpl.html) class that implements that interface.

### Deprecated / Removed Methods

The static deprecated methods in [`play.i18n.Messages`](api/java/play/i18n/Messages.html) have been removed in 2.6.x, as there are equivalent methods on the [`MessagesApi`](api/java/play/i18n/MessagesApi.html) instance.

## Scala API

### Refactored Messages API to traits

 The `play.api.i18n` package has changed to make access to [`Messages`](api/scala/play/api/i18n/Messages.html) instances easier and reduce the number of implicits in play.  These changes should be transparent to the user, but are provided here for teams extending the I18N API.

[`Messages`](api/scala/play/api/i18n/Messages.html) is now a trait (rather than a case class).  The case class is now [`MessagesImpl`](api/scala/play/api/i18n/MessagesImpl.html), which implements [`Messages`](api/scala/play/api/i18n/Messages.html).

### Smoother I18nSupport

Using a form inside a controller is a smoother experience in 2.6.x.   [`ControllerComponents`](api/scala/play/api/mvc/ControllerComponents.html) contains a [`MessagesApi`](api/scala/play/api/i18n/MessagesApi.html) instance, which is exposed by [`AbstractController`](api/scala/play/api/mvc/AbstractController.html).  This means that the [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) trait does not require an explicit `val messagesApi: MessagesApi` declaration.

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
 
### MessagesProvider trait
 
A new [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) trait is available, which exposes a [`Messages`](api/scala/play/api/i18n/Messages.html) instance.

```scala
trait MessagesProvider {
  def messages: Messages
}
```

[`MessagesImpl`](api/scala/play/api/i18n/MessagesImpl.html) implements [`Messages`](api/scala/play/api/i18n/Messages.html) and [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html), and returns itself by default.

All the template helpers now take [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) as an implicit parameter, rather than a straight `Messages` object, i.e. `inputText.scala.html` takes the following:

```twirl
@(field: play.api.data.Field, args: (Symbol,Any)*)(implicit handler: FieldConstructor, messagesProvider: play.api.i18n.MessagesProvider)
```
 
The benefit to using a [`MessagesProvider`](api/scala/play/api/i18n/MessagesProvider.html) is to loosen the binding so that a [`Messages`](api/scala/play/api/i18n/Messages.html) instance can be produced from other locations without involving [`I18nSupport`](api/scala/play/api/i18n/I18nSupport.html) type enrichment:

```scala
class MessagesRequest[A](request: Request[A], val messages: Messages)
  extends WrappedRequest(request) with play.api.i18n.MessagesProvider {
    def lang: Lang = messages.lang
  }

abstract class AbstractMessagesController(cc: ControllerComponents)
  extends AbstractController(cc) {

  def MessagesAction: ActionBuilder[MessagesRequest, AnyContent] = {
    cc.actionBuilder.andThen(new ActionTransformer[Request, MessagesRequest] {
      def transform[A](request: Request[A]) = Future.successful {
        val messages = cc.messagesApi.preferred(request)
        new MessagesRequest(request, messages)
      }
      override protected def executionContext: ExecutionContext = cc.executionContext
    })
  }
}

class MessagesController @Inject() (
  addToken: CSRFAddToken,
  checkToken: CSRFCheck,
  components: ControllerComponents
) extends AbstractMessagesController(components) {

  import play.api.data.Form
  import play.api.data.Forms._

  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(UserData.apply)(UserData.unapply)
  )

  def index = addToken {
    MessagesAction { implicit request =>
      Ok(views.html.formpage(userForm))
    }
  }

  def userPost = checkToken {
    MessagesAction { implicit request =>
      userForm.bindFromRequest.fold(
        formWithErrors => {
          play.api.Logger.info(s"unsuccessful user submission")
          BadRequest(views.html.formpage(formWithErrors))
        },
        user => {
          play.api.Logger.info(s"successful user submission ${user}")
          Redirect(routes.MessagesController.index()).flashing("success" -> s"User is ${user}!")
        }
      )
    }
  }
}
```

This is also useful for passing around a single implicit request, especially when CSRF checks are involved:

```twirl
@(userForm: Form[UserData])(implicit request: MessagesRequest[_])

@helper.form(action = routes.MessagesController.userPost()) {
    @views.html.helper.CSRF.formField
    @helper.inputText(userForm("name"))
    @helper.inputText(userForm("age"))
    <input type="submit" value="SUBMIT"/>
}
```

Please see [[Passing Messages to Form Helpers|ScalaForms]] for more details.

### Deprecated Methods

`play.api.i18n.Messages.Implicits.applicationMessagesApi` and `play.api.i18n.Messages.Implicits.applicationMessages` have been deprecated, because they rely on an implicit `Application` instance.

The `play.api.mvc.Controller.request2lang` method has been deprecated, because it was using a global `Application` under the hood.

The `play.api.i18n.I18nSupport.request2Messages` implicit conversion method has been moved to `I18NSupportLowPriorityImplicits.request2Messages`, and deprecated in favor of `request.messages` type enrichment, which is clearer overall.

The `I18NSupportLowPriorityImplicits.lang2Messages` implicit conversion has been moved out to `LangImplicits.lang2Messages`, because of confusion when both implicit Request and a Lang were in scope. Please extend the [`play.api.i18n.LangImplicits`](api/scala/play/api/i18n/LangImplicits.html) trait specifically if you want to use i18n with an implicit Lang.
