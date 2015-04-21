/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka;

public class ParentActorProtocol {

    public static class GetChild {
        public final String key;

        public GetChild(String key) {
            this.key = key;
        }
    }
}
