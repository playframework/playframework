/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.csrf;

// #filters
import javax.inject.Inject;
import play.filters.csrf.CSRFFilter;
import play.http.DefaultHttpFilters;

public class Filters extends DefaultHttpFilters {
  @Inject
  public Filters(CSRFFilter csrfFilter) {
    super(csrfFilter);
  }
}
// #filters
