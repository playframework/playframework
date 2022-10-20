/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import java.util.Collections;
import java.util.List;
import play.components.HttpComponents;
import play.mvc.EssentialFilter;

/**
 * Java component to mix in when no default filters should be mixed in to {@link
 * play.BuiltInComponents}.
 *
 * <p>Usage:
 *
 * <pre>
 * public class MyComponents extends BuiltInComponentsFromContext implements NoHttpFiltersComponents {
 *
 *    public MyComponents(ApplicationLoader.Context context) {
 *        super(context);
 *    }
 *
 *    // required methods implementation
 *
 * }
 * </pre>
 *
 * @see HttpFiltersComponents#httpFilters()
 */
public interface NoHttpFiltersComponents extends HttpComponents {

  @Override
  default List<EssentialFilter> httpFilters() {
    return Collections.emptyList();
  }
}
