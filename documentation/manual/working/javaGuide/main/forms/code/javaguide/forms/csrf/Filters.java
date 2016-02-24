/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.forms.csrf;

//#filters
import play.http.HttpFilters;
import play.mvc.EssentialFilter;
import play.filters.csrf.CSRFFilter;
import javax.inject.Inject;

public class Filters implements HttpFilters {

    @Inject CSRFFilter csrfFilter;

    @Override
    public EssentialFilter[] filters() {
        return new EssentialFilter[] { csrfFilter.asJava() };
    }
}
//#filters
