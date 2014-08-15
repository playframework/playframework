/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.advanced.di;

import play.test.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class JavaDependencyInjection extends WithApplication {

    @Test
    public void cleanup() {
        app.injector().instanceOf(java8guide.advanced.di.MessageQueueConnection.class);
        stopPlay();
        assertTrue(MessageQueue.stopped);
    }
}
