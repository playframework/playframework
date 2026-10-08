/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import com.codeborne.selenide.impl.AttachmentPrinter;
import com.codeborne.selenide.impl.Clock;
import com.codeborne.selenide.impl.ScreenShotLaboratory;
import com.codeborne.selenide.impl.Screenshot;
import com.codeborne.selenide.impl.WebPageSourceExtractor;
import com.codeborne.selenide.impl.WebdriverPhotographer;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A screenshot laboratory owned by a single test browser.
 *
 * <p>Selenide's default laboratory is a JVM-wide singleton that also records screenshots per thread
 * and per test "context" in {@link ThreadLocal}s. This laboratory overrides every method which
 * reads or writes those thread locals and only keeps the screenshots of its own browser.
 */
final class NoThreadLocalScreenShotLaboratory extends ScreenShotLaboratory {

  // Shared by all browsers, like the counter of Selenide's JVM-wide default laboratory, so browsers
  // taking a screenshot at the same millisecond don't overwrite each other's files
  private static final AtomicLong fileCounter = new AtomicLong();

  NoThreadLocalScreenShotLaboratory() {
    super(
        new WebdriverPhotographer(),
        new WebPageSourceExtractor(),
        new AttachmentPrinter(),
        new Clock());
  }

  @Override
  protected String generateScreenshotFileName() {
    return clock.timestamp() + "." + fileCounter.getAndIncrement();
  }

  @Override
  protected void addToHistory(Screenshot screenshot) {
    synchronized (allScreenshots) {
      allScreenshots.add(screenshot);
    }
  }

  @Override
  public List<Screenshot> screenshots() {
    synchronized (allScreenshots) {
      return List.copyOf(allScreenshots);
    }
  }

  @Override
  public void startContext(String context) {
    // there is no context: all screenshots belong to this laboratory's browser
  }

  @Override
  public List<Screenshot> finishContext() {
    return screenshots();
  }

  @Override
  public List<File> getThreadScreenshots() {
    return getScreenshots();
  }

  @Override
  public List<Screenshot> threadScreenshots() {
    return screenshots();
  }

  @Override
  public List<File> getContextScreenshots() {
    return getScreenshots();
  }

  @Override
  public List<Screenshot> contextScreenshots() {
    return screenshots();
  }
}
