/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.extending;

public class MyComponent {
    public boolean started;
    public boolean stopped;
    public void start() {
        started = true;
    }
    public void stop() {
        stopped = true;
    }
}
