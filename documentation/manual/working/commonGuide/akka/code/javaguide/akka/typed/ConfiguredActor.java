/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed;

// #oo-configured-actor
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Adapter;
import com.typesafe.config.Config;

import javax.inject.Inject;

public final class ConfiguredActor extends AbstractBehavior<ConfiguredActor.GetConfig> {

  public static final class GetConfig {
    public final ActorRef<String> replyTo;

    public GetConfig(ActorRef<String> replyTo) {
      this.replyTo = replyTo;
    }
  }

  private final String config;

  @Inject
  public ConfiguredActor(Config configuration) {
    config = configuration.getString("my.config");
  }

  @Override
  public Receive<GetConfig> createReceive() {
    return newReceiveBuilder().onMessage(GetConfig.class, this::onGetConfig).build();
  }

  private Behavior<GetConfig> onGetConfig(GetConfig message) {
    message.replyTo.tell(config);
    return this;
  }
}
// #oo-configured-actor
