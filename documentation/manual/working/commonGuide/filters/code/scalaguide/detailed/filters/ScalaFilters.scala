/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.detailed.filters

// #filters-combine-enabled-filters
import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.api.http.EnabledFilters
import play.filters.cors.CORSFilter

class Filters @Inject() (enabledFilters: EnabledFilters, corsFilter: CORSFilter)
    extends DefaultHttpFilters(enabledFilters.filters :+ corsFilter: _*)

// #filters-combine-enabled-filters
