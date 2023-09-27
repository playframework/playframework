/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko;

// #injectedparent

import javax.inject.Inject;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import play.libs.pekko.InjectedActorSupport;

public class ParentActor extends AbstractActor implements InjectedActorSupport {

  private ConfiguredChildActorProtocol.Factory childFactory;

  @Inject
  public ParentActor(ConfiguredChildActorProtocol.Factory childFactory) {
    this.childFactory = childFactory;
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder().match(ParentActorProtocol.GetChild.class, this::getChild).build();
  }

  private void getChild(ParentActorProtocol.GetChild msg) {
    String key = msg.key;
    ActorRef child = injectedChild(() -> childFactory.create(key), key);
    sender().tell(child, self());
  }
}
// #injectedparent
