//###replace: package controllers.admin;
package javaguide.common.build.controllers;

import play.mvc.BaseController;
import play.mvc.Result;

public class HomeController extends BaseController {
    public Result index() {
        return ok("admin");
    }
}
