/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

public class ParentActorProtocol {

  public static class GetChild {
    public final String key;

    public GetChild(String key) {
      this.key = key;
    }
  }
}
