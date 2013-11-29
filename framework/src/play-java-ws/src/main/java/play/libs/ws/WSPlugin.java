/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

import play.Application;
import play.Plugin;

/**
 * An extension of Plugin for WS.
 */
public abstract class WSPlugin extends Plugin {

    protected final Application app;

    public WSPlugin(Application app) {
        this.app = app;
    }

    /**
     * The root api of the plugin.
     */
    public abstract WSAPI api();

    /**
     * Used for testing.
     * @return true if the plugin loaded successfully, false otherwise.
     */
    protected abstract boolean loaded();
}
