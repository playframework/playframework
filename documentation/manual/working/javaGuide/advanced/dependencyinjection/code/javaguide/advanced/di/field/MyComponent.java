/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.advanced.di.field;

//#field
import javax.inject.*;
import play.libs.ws.*;

public class MyComponent {
    @Inject WSClient ws;

    // ...
}
//#field
