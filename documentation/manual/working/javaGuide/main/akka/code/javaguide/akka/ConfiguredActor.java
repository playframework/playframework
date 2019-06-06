/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

// #injected
import akka.actor.AbstractActor;
import com.typesafe.config.Config;

import javax.inject.Inject;

public class ConfiguredActor extends AbstractActor {

  private Config configuration;

  @Inject
  public ConfiguredActor(Config configuration) {
    this.configuration = configuration;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(
            ConfiguredActorProtocol.GetConfig.class,
            message -> {
              sender().tell(configuration.getString("my.config"), self());
            })
        .build();
  }
}
// #injected
