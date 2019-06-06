/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

// #injectedchild
import akka.actor.AbstractActor;
import com.google.inject.assistedinject.Assisted;
import com.typesafe.config.Config;

import javax.inject.Inject;

public class ConfiguredChildActor extends AbstractActor {

  private final Config configuration;
  private final String key;

  @Inject
  public ConfiguredChildActor(Config configuration, @Assisted String key) {
    this.configuration = configuration;
    this.key = key;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ConfiguredChildActorProtocol.GetConfig.class, this::getConfig)
        .build();
  }

  private void getConfig(ConfiguredChildActorProtocol.GetConfig get) {
    sender().tell(configuration.getString(key), self());
  }
}
// #injectedchild
