//#full-controller
//###replace: package controllers;
package javaguide.http.full;

import play.*;
import play.mvc.*;

public class Application extends Controller {

    public static Result index() {
        return ok("It works!");
    }

}
//#full-controller
