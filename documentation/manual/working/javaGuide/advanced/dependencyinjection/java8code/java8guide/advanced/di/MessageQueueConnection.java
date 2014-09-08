/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.advanced.di;

//#cleanup
import javax.inject.*;
import play.inject.ApplicationLifecycle;
import play.libs.F;

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
