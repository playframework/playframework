Lets implement a simple Auth controller
> **Note** The important part is storing the username in the session so the Security trait works.

    object Auth extends Controller {

      val loginForm = Form(
        tuple(
          "email" -> text,
          "password" -> text
        ) verifying ("Invalid email or password", result => result match {
          case (email, password) => check(email, password)
        })
      )

      def check(username: String, password: String) = {
        (username == "admin" && password == "1234")  
      }

      def login = Action { implicit request =>
        Ok(html.login(loginForm))
      }

      def authenticate = Action { implicit request =>
        loginForm.bindFromRequest.fold(
          formWithErrors => BadRequest(html.login(formWithErrors)),
          user => Redirect(routes.Application.index).withSession(Security.username -> user._1)
        )
      }

      def logout = Action {
        Redirect(routes.Auth.login).withNewSession.flashing(
          "success" -> "You are now logged out."
        )
      }
    }

We submit the login form to `authentication` the validation kicks in and calls the `check` method and if that returns true we redirect to `routes.Application.index` setting the `username` session variable.

Have a look at the [login view](https://github.com/playframework/Play20/blob/master/samples/scala/zentasks/app/views/login.scala.html) from zentasks to see how the form is implemented.

> **What is Security.username:** This method checks if there is a config variable called `session.username` and if not, falls back to using `username` as the session variable name.

## Authorization
We need to implement the [play.api.mvc.Security](https://github.com/playframework/Play20/blob/master/framework/src/play/src/main/scala/play/api/mvc/Security.scala) trait:

    trait Secured {

      def username(request: RequestHeader) = request.session.get(Security.username)

      def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

      def withAuth(f: => String => Request[AnyContent] => Result) = {
        Security.Authenticated(username, onUnauthorized) { user =>
          Action(request => f(user)(request))
        }
      }

      /**
       * This method shows how you could wrap the withAuth method to also fetch your user
       * You will need to implement UserDAO.findOneByUsername
       */
      def withUser(f: User => Request[AnyContent] => Result) = withAuth { username => implicit request =>
        UserDAO.findOneByUsername(username).map { user =>
          f(user)(request)
        }.getOrElse(onUnauthorized(request))
      }
    }

> The Secured trait can live in the same file as the Auth controller

And to use it:

    object Application extends Controller with Secured {

      def index = withAuth { username => implicit request =>
        Ok(html.index(username))
      }

      def user() = withUser { user => implicit request =>
        val username = user.username
        Ok(html.user(user))
      }
    }

 * For an example check out the [zentask sample](https://github.com/playframework/Play20/tree/master/samples/scala/zentasks)
 * There is also a more advanced security plugin called [Deadbolt](https://github.com/schaloner/deadbolt-2)
