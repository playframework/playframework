/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di;

import play.test.*;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class JavaDependencyInjection extends WithApplication {

    @Test
    public void fieldInjection() {
        assertNotNull(app.injector().instanceOf(javaguide.advanced.di.field.MyComponent.class));
    }

    @Test
    public void constructorInjection() {
        assertNotNull(app.injector().instanceOf(javaguide.advanced.di.constructor.MyComponent.class));
    }

    @Test
    public void singleton() {
        app.injector().instanceOf(CurrentSharePrice.class).set(10);
        assertThat(app.injector().instanceOf(CurrentSharePrice.class).get(), equalTo(10));
    }

    @Test
    public void cleanup() {
        app.injector().instanceOf(MessageQueueConnection.class);
        stopPlay();
        assertTrue(MessageQueue.stopped);
    }

    @Test
    public void implementedBy() {
        assertThat(app.injector().instanceOf(Hello.class).sayHello("world"), equalTo("Hello world"));
    }
}
