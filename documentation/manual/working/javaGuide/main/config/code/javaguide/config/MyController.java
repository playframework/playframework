/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
// ###replace: package controllers
package javaguide.config;

import com.typesafe.config.Config;
import play.mvc.Controller;

import javax.inject.Inject;

public class MyController extends Controller {

  private final Config config;

  @Inject
  public MyController(Config config) {
    this.config = config;
  }
}
