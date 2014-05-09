/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
//#code
//###replace: package plugins;
package javaguide.advanced.extending;

import play.Plugin;
import play.Application;

public class MyPlugin extends Plugin {

    private final MyComponent myComponent = new MyComponent();

    public MyPlugin(Application app) {
    }

    public void onStart() {
        myComponent.start();
    }

    public void onStop() {
        myComponent.stop();
    }

    public boolean enabled() {
        return true;
    }

    public MyComponent getMyComponent() {
        return myComponent;
    }
}
//#code
