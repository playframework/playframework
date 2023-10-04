/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko;

public class ParentActorProtocol {

  public static class GetChild {
    public final String key;

    public GetChild(String key) {
      this.key = key;
    }
  }
}
