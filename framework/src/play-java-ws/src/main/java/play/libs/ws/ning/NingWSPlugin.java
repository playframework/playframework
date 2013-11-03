/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import play.Application;
import play.libs.ws.WSAPI;
import play.libs.ws.WSPlugin;

public class NingWSPlugin extends WSPlugin {

    private boolean isLoaded;

    private NingWSAPI ningAPI;

    public NingWSPlugin(Application app) {
        super(app);
    }

    @Override
    public void onStart() {
        super.onStart();
        ningAPI = new NingWSAPI(app);
        isLoaded = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        ningAPI.resetClient();
        isLoaded = false;
    }

    @Override
    public WSAPI api() {
        return ningAPI;
    }

    @Override
    protected boolean loaded() {
        return isLoaded;
    }

}
