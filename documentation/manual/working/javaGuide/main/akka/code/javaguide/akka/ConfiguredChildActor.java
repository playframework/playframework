/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

//#injectedchild
import akka.actor.UntypedAbstractActor;
import com.google.inject.assistedinject.Assisted;
import com.typesafe.config.Config;

import javax.inject.Inject;

public class ConfiguredChildActor extends UntypedAbstractActor {

    private final Config configuration;
    private final String key;

    @Inject
    public ConfiguredChildActor(Config configuration, @Assisted String key) {
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
