/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka;

// #protocol
import akka.actor.Actor;

public class ConfiguredChildActorProtocol {

  public static class GetConfig {}

  public interface Factory {
    public Actor create(String key);
  }
}
// #protocol
