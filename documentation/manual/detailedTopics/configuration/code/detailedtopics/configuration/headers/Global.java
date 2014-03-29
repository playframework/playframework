/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package detailedtopics.configuration.headers;

//#global
import play.GlobalSettings;
import play.api.mvc.EssentialFilter;
import play.filters.headers.SecurityHeadersFilter;

public class Global extends GlobalSettings {
    public <T extends EssentialFilter> Class<T>[] filters() {
        return new Class[]{SecurityHeadersFilter.class};
    }
}
//#global
