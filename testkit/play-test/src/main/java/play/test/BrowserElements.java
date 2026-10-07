/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.test;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.and;
import static com.codeborne.selenide.Condition.clickable;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;

import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import java.util.function.Consumer;
import org.openqa.selenium.By;

/**
 * All elements matching a selector, as returned by {@link AbstractTestBrowser#$(String)} and {@link
 * AbstractTestBrowser#find(String)}.
 *
 * <p>A Selenide {@link ElementsCollection} which additionally provides the actions on all its
 * elements that Play's test browser always offered: {@link #click()}, {@link #submit()}, {@link
 * #fill()} and {@link #write(String...)}. Like all Selenide collections, the elements are looked up
 * lazily, and the actions wait until there is at least one element they can act on.
 */
public class BrowserElements extends ElementsCollection {

  private static final WebElementCondition fillable = and("visible and enabled", visible, enabled);

  BrowserElements(Driver driver, String cssSelector) {
    super(driver, cssSelector);
  }

  BrowserElements(Driver driver, By locator) {
    super(driver, locator);
  }

  /**
   * Clicks all clickable elements. To click a single element, use {@code first().click()} or {@link
   * AbstractTestBrowser#el(String)}.
   *
   * @return these elements.
   */
  public BrowserElements click() {
    forEach(clickable, element -> element.click());
    return this;
  }

  /**
   * Submits all enabled elements (usually a form, or an element within a form).
   *
   * @return these elements.
   */
  public BrowserElements submit() {
    forEach(enabled, element -> element.submit());
    return this;
  }

  /**
   * Fills all visible and enabled elements, e.g. {@code browser.$("#email").fill().with("x")}.
   *
   * @return the fill action.
   * @see #write(String...)
   */
  public Fill fill() {
    return new Fill(this);
  }

  /**
   * Sets the values of all visible and enabled elements, like FluentLenium did: the first visible
   * element gets the first value, the second visible element the second value and so on, but
   * disabled elements are skipped. If there are more visible elements than values, the remaining
   * elements get the last value.
   *
   * @param values the values.
   * @return these elements.
   */
  public BrowserElements write(String... values) {
    if (values.length > 0) {
      withAtLeastOne(fillable);
      int index = 0;
      for (SelenideElement element : filterBy(visible).asFixedIterable()) {
        String value = values[Math.min(index++, values.length - 1)];
        if (element.isEnabled()) {
          element.setValue(value);
        }
      }
    }
    return this;
  }

  private void forEach(WebElementCondition condition, Consumer<SelenideElement> action) {
    for (SelenideElement element : withAtLeastOne(condition)) {
      action.accept(element);
    }
  }

  // Waits until at least one element matches the condition, and returns the matching elements
  private Iterable<SelenideElement> withAtLeastOne(WebElementCondition condition) {
    ElementsCollection matching = filterBy(condition);
    matching.shouldHave(sizeGreaterThan(0));
    return matching.asFixedIterable();
  }

  /** Fills elements with values, see {@link BrowserElements#fill()}. */
  public static final class Fill {
    private final BrowserElements elements;

    private Fill(BrowserElements elements) {
      this.elements = elements;
    }

    /**
     * Sets the values of the elements, see {@link BrowserElements#write(String...)}.
     *
     * @param values the values.
     * @return this fill action.
     */
    public Fill with(String... values) {
      elements.write(values);
      return this;
    }

    /**
     * Synonym for {@link #with(String...)}.
     *
     * @param values the values.
     * @return this fill action.
     */
    public Fill withText(String... values) {
      return with(values);
    }
  }
}
