/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5.guice;

// #mock-component
public class MockComponent implements Component {
  public String hello() {
    return "mock";
  }
}
// #mock-component
