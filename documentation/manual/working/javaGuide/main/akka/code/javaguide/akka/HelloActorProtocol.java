/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
// #protocol
// ###replace: package actors;
package javaguide.akka;

public class HelloActorProtocol {

  public static class SayHello {
    public final String name;

    public SayHello(String name) {
      this.name = name;
    }
  }
}
// #protocol
