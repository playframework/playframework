/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko;

// #injected
import com.typesafe.config.Config;
import javax.inject.Inject;
import org.apache.pekko.actor.AbstractActor;

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
