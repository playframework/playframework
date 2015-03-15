/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.forms.csrf;

//#filters
import play.http.HttpFilters;
import play.api.mvc.EssentialFilter;
import play.filters.csrf.CSRFFilter;
import javax.inject.Inject;

public class Filters implements HttpFilters {

    @Inject CSRFFilter csrfFilter;

    @Override
    public EssentialFilter[] filters() {
        return new EssentialFilter[] { csrfFilter };
    }
}
//#filters
