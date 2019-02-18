/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests.guice;

// #default-component
public class DefaultComponent implements Component {
  public String hello() {
    return "default";
  }
}
// #default-component
