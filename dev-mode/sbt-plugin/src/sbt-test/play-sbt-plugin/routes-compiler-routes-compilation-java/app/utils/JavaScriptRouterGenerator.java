/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package utils;

import static controllers.routes.javascript.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import play.libs.Scala;
import scala.Option;

public class JavaScriptRouterGenerator {

  public static void main(String[] args) throws IOException {
    var jsFile =
        play.api.routing.JavaScriptReverseRouter.apply(
                "jsRoutes",
                Option.empty(),
                "localhost",
                Scala.varargs(
                    Assets.versioned(),
                    Application.index(),
                    Application.post(),
                    Application.withParam(),
                    Application.takeBool(),
                    Application.takeOptionalInt(),
                    Application.takeOptionalIntWithDefault()))
            .body();

    // Add module exports for node
    var jsModule = jsFile + "\nmodule.exports = jsRoutes";

    var path = Paths.get(args[0]);
    Files.createDirectories(path.getParent());
    Files.writeString(path, jsModule);
  }
}
