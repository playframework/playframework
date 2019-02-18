/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.di;

// #implemented-by
public class EnglishHello implements Hello {

  public String sayHello(String name) {
    return "Hello " + name;
  }
}
// #implemented-by
