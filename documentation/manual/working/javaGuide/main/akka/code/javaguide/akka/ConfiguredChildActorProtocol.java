/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka;

//#protocol
import akka.actor.Actor;

public class ConfiguredChildActorProtocol {

    public static class GetConfig {}

    public interface Factory {
        public Actor create(String key);
    }
}
//#protocol
