/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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

        lifecycle.addStopHook(() -> {
            connection.stop();
            return F.Promise.pure(null);
        });
    }

    // ...
}
//#cleanup
