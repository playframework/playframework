/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.validation.MessageInterpolator;
import java.util.Locale;
import org.junit.Test;

public class FormMessageInterpolatorTest {

  private final MessageInterpolator delegate = mock(MessageInterpolator.class);
  private final MessageInterpolator.Context context = mock(MessageInterpolator.Context.class);
  private final MessageInterpolator interpolator =
      new Form.FixedLocaleMessageInterpolator(delegate, Locale.GERMAN);

  @Test
  public void shouldUseDefaultLocaleWhenNoLocaleIsProvided() {
    interpolator.interpolate("message", context);

    verify(delegate).interpolate("message", context, Locale.GERMAN);
  }

  @Test
  public void shouldHonorExplicitLocale() {
    interpolator.interpolate("message", context, Locale.FRENCH);

    verify(delegate).interpolate("message", context, Locale.FRENCH);
  }
}
