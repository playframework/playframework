/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
