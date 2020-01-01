/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.typed.fp;

// #fp-configured-actor
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.typesafe.config.Config;

import javax.inject.Inject;

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
