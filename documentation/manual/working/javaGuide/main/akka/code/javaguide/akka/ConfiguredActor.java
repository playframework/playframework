/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.akka;

//#injected
import akka.actor.UntypedActor;
import com.typesafe.config.Config;

import javax.inject.Inject;

public class ConfiguredActor extends UntypedActor {

    private Config configuration;

    @Inject
    public ConfiguredActor(Config configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ConfiguredActorProtocol.GetConfig) {
            sender().tell(configuration.getString("my.config"), self());
        }
    }
}
//#injected
