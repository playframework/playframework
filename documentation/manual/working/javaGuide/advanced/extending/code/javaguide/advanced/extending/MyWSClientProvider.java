/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.extending;

import play.libs.ws.WSClient;

public class MyWSClientProvider implements javax.inject.Provider<WSClient> {
    @Override
    public WSClient get() {
        return new MyWSClient();
    }
}
