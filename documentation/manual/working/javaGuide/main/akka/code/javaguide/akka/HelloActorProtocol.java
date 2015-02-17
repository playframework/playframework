/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
//#protocol
//###replace: package actors;
package javaguide.akka;

public class HelloActorProtocol {

    public static class SayHello {
        public final String name;

        public SayHello(String name) {
            this.name = name;
        }
    }
}
//#protocol
