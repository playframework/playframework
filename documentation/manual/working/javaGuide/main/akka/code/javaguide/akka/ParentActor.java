/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

//#injectedparent
import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;
import play.libs.akka.InjectedActorSupport;

import javax.inject.Inject;

public class ParentActor extends UntypedAbstractActor implements InjectedActorSupport {

    private ConfiguredChildActorProtocol.Factory childFactory;

    @Inject
    public ParentActor(ConfiguredChildActorProtocol.Factory childFactory) {
        this.childFactory = childFactory;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ParentActorProtocol.GetChild) {
            String key = ((ParentActorProtocol.GetChild) message).key;
            ActorRef child = injectedChild(() -> childFactory.create(key), key);
            sender().tell(child, self());
        }
    }
}
//#injectedparent
