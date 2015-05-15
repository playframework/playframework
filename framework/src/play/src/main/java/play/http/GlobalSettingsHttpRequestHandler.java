/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.http;

import play.api.GlobalSettings;
import play.core.j.JavaGlobalSettingsAdapter;
import play.mvc.Action;
import play.mvc.Http;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;

/**
 * Request handler that delegates to global
 */
@Singleton
public class GlobalSettingsHttpRequestHandler extends DefaultHttpRequestHandler {

    private final GlobalSettings global;

    @Inject
    public GlobalSettingsHttpRequestHandler(GlobalSettings global) {
        this.global = global;
    }

    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
        if (global instanceof JavaGlobalSettingsAdapter) {
            return ((JavaGlobalSettingsAdapter) global).underlying().onRequest(request, actionMethod);
        } else {
            return super.createAction(request, actionMethod);
        }
    }
}
