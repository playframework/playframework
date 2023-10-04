/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// #protocol
// ###replace: package actors;
package javaguide.pekko;

public class HelloActorProtocol {

  public static class SayHello {
    public final String name;

    public SayHello(String name) {
      this.name = name;
    }
  }
}
// #protocol
