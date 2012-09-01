package controllers;

import java.util.*;

import play.*;
import play.mvc.*;

import static play.libs.Json.toJson;

public class JavaControllerInstance extends Controller {

  public Result index() {
    Map<String, String> d = new HashMap<String, String>();
    d.put("peter", "foo");
    d.put("yay", "value");
    return ok(toJson(d));
  }

}
