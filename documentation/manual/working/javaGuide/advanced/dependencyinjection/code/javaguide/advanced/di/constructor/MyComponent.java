/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di.constructor;

//#constructor
import javax.inject.*;
import play.libs.ws.*;

public class MyComponent {
    private final WSClient ws;

    @Inject
    public MyComponent(WSClient ws) {
        this.ws = ws;
    }

    // ...
}
//#constructor
