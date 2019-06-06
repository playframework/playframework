/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

// #injectedparent

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import play.libs.akka.InjectedActorSupport;

import javax.inject.Inject;

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
