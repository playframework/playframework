/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko.typed.fp;

// #fp-configured-actor
import com.typesafe.config.Config;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public final class ConfiguredActor {

  public static final class GetConfig {
    public final ActorRef<String> replyTo;

    public GetConfig(ActorRef<String> replyTo) {
      this.replyTo = replyTo;
    }
  }

  public static Behavior<ConfiguredActor.GetConfig> create(Config config) {
    String myConfig = config.getString("my.config");
    return Behaviors.receiveMessage(
        (GetConfig message) -> {
          message.replyTo.tell(myConfig);
          return Behaviors.same();
        });
  }
}
// #fp-configured-actor
