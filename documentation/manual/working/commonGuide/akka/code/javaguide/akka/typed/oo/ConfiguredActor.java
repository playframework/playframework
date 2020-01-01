/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed.oo;

// #oo-configured-actor
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.typesafe.config.Config;

public final class ConfiguredActor extends AbstractBehavior<ConfiguredActor.GetConfig> {

  public static final class GetConfig {
    public final ActorRef<String> replyTo;

    public GetConfig(ActorRef<String> replyTo) {
      this.replyTo = replyTo;
    }
  }

  private final String config;

  public static Behavior<ConfiguredActor.GetConfig> create(Config config) {
    return Behaviors.setup((ctx) -> new ConfiguredActor(ctx, config));
  }

  private ConfiguredActor(ActorContext<ConfiguredActor.GetConfig> context, Config config) {
    super(context);
    this.config = config.getString("my.config");
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
