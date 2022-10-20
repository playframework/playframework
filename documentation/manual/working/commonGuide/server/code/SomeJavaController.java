/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import java.util.Optional;
// #server-request-attribute
import play.api.mvc.request.RequestAttrKey;

public class SomeJavaController extends Controller {

  public Result index(Http.Request request) {
    assert (request
        .attrs()
        .getOptional(RequestAttrKey.Server().asJava())
        .equals(Optional.of("netty")));
    // ...
    // ###skip: 1
    return ok("");
  }
}
// #server-request-attribute
