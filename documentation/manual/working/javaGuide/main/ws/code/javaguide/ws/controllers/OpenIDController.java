package javaguide.ws.controllers;

//#ws-openid-controller
import java.util.HashMap;
import java.util.Map;

import play.data.DynamicForm;
import play.data.Form;
import play.libs.F.Function;
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
    DynamicForm requestData = Form.form().bindFromRequest();
    String openID = requestData.get("openID");

    final Promise<String> redirectUrlPromise =
        openIdClient.redirectURL(openID, routes.OpenIDController.openIDCallback().absoluteURL(request()));

    final Promise<Result> resultPromise = redirectUrlPromise.map(new Function<String, Result>() {
      @Override
      public Result apply(String url) {
        return redirect(url);
      }
    }).recover(new Function<Throwable, Result>() {
      @Override
      public Result apply(Throwable throwable) throws Throwable {
        //###replace:         return badRequest(views.html.login.render(throwable.getMessage()));
        return badRequest(javaguide.ws.html.login.render(throwable.getMessage()));
      }
    });

    return resultPromise;
  }

  public Promise<Result> openIDCallback() {

    final Promise<UserInfo> userInfoPromise = openIdClient.verifiedId();

    final Promise<Result> resultPromise = userInfoPromise.map(new Function<UserInfo, Result>() {
      @Override
      public Result apply(UserInfo userInfo) {
        return ok(userInfo.id() + "\n" + userInfo.attributes());
      }
    }).recover(new Function<Throwable, Result>() {
      @Override
      public Result apply(Throwable throwable) throws Throwable {
        //###replace:         return badRequest(views.html.login.render(throwable.getMessage()));
        return badRequest(javaguide.ws.html.login.render(throwable.getMessage()));
      }
    });

    return resultPromise;
  }

}
//#ws-openid-controller

class OpenIDSamples extends Controller {

  static OpenIdClient openIdClient;

  public static void extendedAttributes() {
    
    final String openID = "";
    
    //#ws-openid-extended-attributes
    final Map<String, String> attributes = new HashMap<String, String>();
    attributes.put("email", "http://schema.openid.net/contact/email");
    
    final Promise<String> redirectUrlPromise = openIdClient.redirectURL(
      openID, 
      routes.OpenIDController.openIDCallback().absoluteURL(request()), 
      attributes
    );
    //#ws-openid-extended-attributes
  }
  
}