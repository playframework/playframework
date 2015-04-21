/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka;

//#injectedchild
import akka.actor.UntypedActor;
import com.google.inject.assistedinject.Assisted;
import play.Configuration;

import javax.inject.Inject;

public class ConfiguredChildActor extends UntypedActor {

    private final Configuration configuration;
    private final String key;

    @Inject
    public ConfiguredChildActor(Configuration configuration, @Assisted String key) {
        this.configuration = configuration;
        this.key = key;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ConfiguredChildActorProtocol.GetConfig) {
            sender().tell(configuration.getString(key), self());
        }
    }
}
//#injectedchild
