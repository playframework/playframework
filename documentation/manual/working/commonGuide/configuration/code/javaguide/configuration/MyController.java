/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// #dependency-injection
// ###replace: package controllers
package javaguide.configuration;

import com.typesafe.config.Config;
import javax.inject.Inject;
import play.mvc.Controller;

public class MyController extends Controller {

  private final Config config;

  @Inject
  public MyController(Config config) {
    this.config = config;
  }
}
// #dependency-injection
