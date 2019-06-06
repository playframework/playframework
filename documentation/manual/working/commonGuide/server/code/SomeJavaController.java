/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import play.mvc.Controller;
import play.mvc.Result;
import java.util.Optional;
// #server-request-attribute
import play.api.mvc.request.RequestAttrKey;

public class SomeJavaController extends Controller {

  public Result index() {
    assert (request()
        .attrs()
        .getOptional(RequestAttrKey.Server().asJava())
        .equals(Optional.of("netty")));
    // ...
    // ###skip: 1
    return ok("");
  }
}
// #server-request-attribute
