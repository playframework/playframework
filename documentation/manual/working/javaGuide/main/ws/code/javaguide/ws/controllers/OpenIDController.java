package javaguide.ws.controllers;

import play.twirl.api.Html;

//#ws-openid-controller
import java.util.*;

import play.data.*;
import play.libs.F.Promise;
import play.libs.openid.*;
import play.mvc.*;

import javax.inject.Inject;

public class OpenIDController extends Controller {

  @Inject OpenIdClient openIdClient;

  public Result login() {
    return ok(views.html.login.render(""));
  }

  public Promise<Result> loginPost() {

    // Form data
    final DynamicForm requestData = Form.form().bindFromRequest();
    final String openID = requestData.get("openID");

    final Promise<String> redirectUrlPromise =
            openIdClient.redirectURL(openID, routes.OpenIDController.openIDCallback().absoluteURL(request()));

    final Promise<Result> resultPromise = redirectUrlPromise
            .map(Controller::redirect)
            .recover(throwable ->
                    badRequest(views.html.login.render(throwable.getMessage()))
            );

    return resultPromise;
  }

  public Promise<Result> openIDCallback() {

    final Promise<UserInfo> userInfoPromise = openIdClient.verifiedId();

    final Promise<Result> resultPromise = userInfoPromise.map(userInfo ->
            (Result) ok(userInfo.id() + "\n" + userInfo.attributes())
    ).recover(throwable ->
            badRequest(views.html.login.render(throwable.getMessage()))
    );

    return resultPromise;
  }

  public static class views {
      public static class html {
          public static class login {
              public static Html render(String msg) {
                  return javaguide.ws.html.login.render(msg);
              }
          }
      }
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