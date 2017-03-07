/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.configuration.allowedhosts.sapi

//#filters
import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.redirectplain.RedirectPlainFilter

class Filters @Inject() (redirectPlainFilter: RedirectPlainFilter)
  extends DefaultHttpFilters(redirectPlainFilter)
//#filters
