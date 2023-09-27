/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko.typed.oo;

// #oo-configured-actor
import com.typesafe.config.Config;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

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
