/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.http.routing.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import play.mvc.Controller;
import play.mvc.Result;

public class Api extends Controller {
  public Result list(String version) {
    return ok("version " + version);
  }

  public Result listOpt(Optional<String> version) {
    return ok("version " + version.orElse("unknown"));
  }

  public Result listItems(List<String> items) {
    return ok("params " + String.join(",", items));
  }

  public Result listIntItems(List<Integer> items) {
    return ok(
        "params "
            + String.join(",", items.stream().map(p -> p.toString()).collect(Collectors.toList())));
  }

  public Result newThing() {
    return ok();
  }
}
