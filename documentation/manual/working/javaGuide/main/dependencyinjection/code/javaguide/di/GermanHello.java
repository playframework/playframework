/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

public class GermanHello implements Hello {
  @Override
  public String sayHello(String name) {
    return "Hallo " + name;
  }
}
