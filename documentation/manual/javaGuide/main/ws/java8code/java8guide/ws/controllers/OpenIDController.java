package java8guide.ws.controllers;

//#ws-openid-controller
import javaguide.ws.controllers.routes;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F.Promise;
import play.libs.openid.OpenID;
import play.libs.openid.OpenID.UserInfo;
import play.mvc.Controller;
import play.mvc.Result;

public class OpenIDController extends Controller {

  public static Result login() {
    //###replace:     return ok(views.html.login.render(""));
    return ok(javaguide.ws.html.login.render(""));
  }

  public static Promise<Result> loginPost() {
    
    // Form data
    final DynamicForm requestData = Form.form().bindFromRequest();
    final String openID = requestData.get("openID");
    
    final Promise<String> redirectUrlPromise =
        OpenID.redirectURL(openID, routes.OpenIDController.openIDCallback().absoluteURL(request()));

    final Promise<Result> resultPromise = redirectUrlPromise.map(url -> {
      return redirect(url);
    }).recover(throwable -> {
      //###replace:         return badRequest(views.html.login.render(throwable.getMessage()));
      return badRequest(javaguide.ws.html.login.render(throwable.getMessage()));
    });

    return resultPromise;
  }

  public static Promise<Result> openIDCallback() {

    final Promise<UserInfo> userInfoPromise = OpenID.verifiedId();
    
    final Promise<Result> resultPromise = userInfoPromise.map(userInfo -> {
      return (Result) ok(userInfo.id + "\n" + userInfo.attributes);
    }).recover(throwable -> {
      //###replace:         return badRequest(views.html.login.render(throwable.getMessage()));
      return badRequest(javaguide.ws.html.login.render(throwable.getMessage()));
    });
    
    return resultPromise;
  }

}
//#ws-openid-controller