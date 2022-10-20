/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

// #implemented-by
public class EnglishHello implements Hello {

  public String sayHello(String name) {
    return "Hello " + name;
  }
}
// #implemented-by
