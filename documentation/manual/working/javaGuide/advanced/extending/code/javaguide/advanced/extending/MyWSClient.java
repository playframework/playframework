/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.extending;

import java.io.IOException;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

public class MyWSClient implements WSClient {
    @Override
    public Object getUnderlying() {
        return null;
    }

    @Override
    public play.api.libs.ws.WSClient asScala() {
        return null;
    }

    @Override
    public WSRequest url(String url) {
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}
