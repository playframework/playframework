/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka;

//#injectedparent
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import play.libs.akka.InjectedActorSupport;

import javax.inject.Inject;

public class ParentActor extends UntypedActor implements InjectedActorSupport {

    @Inject ConfiguredChildActorProtocol.Factory childFactory;

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
