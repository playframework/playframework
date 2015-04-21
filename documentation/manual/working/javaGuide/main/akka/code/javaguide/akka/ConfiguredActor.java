package javaguide.akka;

//#injected
import akka.actor.UntypedActor;
import play.Configuration;

import javax.inject.Inject;

public class ConfiguredActor extends UntypedActor {

    @Inject Configuration configuration;

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof ConfiguredActorProtocol.GetConfig) {
            sender().tell(configuration.getString("my.config"), self());
        }
    }
}
//#injected
