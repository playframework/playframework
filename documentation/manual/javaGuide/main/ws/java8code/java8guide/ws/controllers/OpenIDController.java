package java8guide.ws.controllers;

import javaguide.ws.controllers.routes;

//#ws-openid-controller
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F.Promise;
import play.libs.openid.*;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

public class OpenIDController extends Controller {

  @Inject OpenIdClient openIdClient;

  public Result login() {
    //###replace:     return ok(views.html.login.render(""));
    return ok(javaguide.ws.html.login.render(""));
  }

  public Promise<Result> loginPost() {
    
    // Form data
    final DynamicForm requestData = Form.form().bindFromRequest();
    final String openID = requestData.get("openID");
    
    final Promise<String> redirectUrlPromise =
        openIdClient.redirectURL(openID, routes.OpenIDController.openIDCallback().absoluteURL(request()));

    final Promise<Result> resultPromise = redirectUrlPromise.map(url -> {
      return redirect(url);
    }).recover(throwable -> {
      //###replace:         return badRequest(views.html.login.render(throwable.getMessage()));
      return badRequest(javaguide.ws.html.login.render(throwable.getMessage()));
    });

    return resultPromise;
  }

  public Promise<Result> openIDCallback() {

    final Promise<UserInfo> userInfoPromise = openIdClient.verifiedId();
    
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