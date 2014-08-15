/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di;

//#cleanup
import javax.inject.*;
import play.inject.ApplicationLifecycle;
import play.libs.F;

import java.util.concurrent.Callable;

@Singleton
public class MessageQueueConnection {
    private final MessageQueue connection;

    @Inject
    public MessageQueueConnection(ApplicationLifecycle lifecycle) {
        connection = MessageQueue.connect();

        lifecycle.addStopHook(new Callable<F.Promise<Void>>() {
            public F.Promise<Void> call() throws Exception {
                connection.stop();
                return F.Promise.pure(null);
            }
        });
    }

    // ...
}
//#cleanup
