/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
//#code
//###replace: package plugins;
package javaguide.advanced.extending;

import play.Plugin;

public class MyPlugin extends Plugin {

    private final MyComponent myComponent = new MyComponent();

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
