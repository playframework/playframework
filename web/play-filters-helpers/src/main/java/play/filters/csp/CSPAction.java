/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.csp;

import jakarta.inject.Inject;

/**
 * This action is used to add a CSP header to the response through injection.
 *
 * <p>Normally you would use the annotation {@code @CSP} on your action rather than use this
 * directly.
 */
public class CSPAction extends AbstractCSPAction {

  private final CSPProcessor processor;

  @Inject
  public CSPAction(CSPProcessor processor) {
    this.processor = processor;
  }

  @Override
  public CSPProcessor processor() {
    return processor;
  }
}
