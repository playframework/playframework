/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.extending;

import akka.actor.*;

public class MyActor extends UntypedActor {
    public static Props props() {
        return Props.create(MyActor.class);
    }

    public void onReceive(Object message) throws Exception {
        sender().tell(message, self());
    }
}
