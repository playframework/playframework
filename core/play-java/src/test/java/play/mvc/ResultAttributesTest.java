/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import play.libs.typedmap.TypedEntry;
import play.libs.typedmap.TypedKey;
import play.libs.typedmap.TypedMap;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public final class ResultAttributesTest {

  @Parameters
  public static Collection<Result> targets() {
    return Arrays.asList(Results.ok());
  }

  private Result result;

  public ResultAttributesTest(final Result result) {
    this.result = result;
  }

  @Test
  public void testResult_emptyByDefault() {
    assertEquals(TypedMap.empty(), result.attrs());
  }

  @Test
  public void testResult_addSingleAttribute() {
    final TypedKey<String> color = TypedKey.create("color");

    final Result newResult = result.addAttr(color, "red");

    assertTrue(newResult.attrs().containsKey(color));
    assertEquals("red", newResult.attrs().get(color));
  }

  @Test
  public void testResult_KeepCurrentAttributesWhenAddingANewOne() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Result newResult = result.addAttr(color, "red").addAttr(number, 5L);

    assertTrue(newResult.attrs().containsKey(number));
    assertTrue(newResult.attrs().containsKey(color));
    assertEquals(((Long) 5L), newResult.attrs().get(number));
    assertEquals("red", newResult.attrs().get(color));
  }

  @Test
  public void testResult_OverrideExistingValue() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Result newResult = result.addAttr(color, "red").addAttr(number, 5L).addAttr(color, "white");

    assertTrue(newResult.attrs().containsKey(number));
    assertTrue(newResult.attrs().containsKey(color));
    assertEquals(((Long) 5L), newResult.attrs().get(number));
    assertEquals("white", newResult.attrs().get(color));
  }

  @Test
  public void testResult_addMultipleAttributes() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    final Result newResult =
        result.addAttrs(new TypedEntry<>(color, "red"), new TypedEntry<>(number, 3L));

    assertTrue(newResult.attrs().containsKey(color));
    assertTrue(newResult.attrs().containsKey(number));
    assertEquals("red", newResult.attrs().get(color));
    assertEquals((Long) 3L, newResult.attrs().get(number));
  }

  @Test
  public void testResult_KeepCurrentAttributesWhenAddingMultipleOnes() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");
    final TypedKey<String> direction = TypedKey.create("direction");

    Result newResult =
        result
            .addAttr(color, "red")
            .addAttrs(new TypedEntry<>(number, 5L), new TypedEntry<>(direction, "left"));

    assertTrue(newResult.attrs().containsKey(number));
    assertTrue(newResult.attrs().containsKey(direction));
    assertTrue(newResult.attrs().containsKey(color));
    assertEquals(((Long) 5L), newResult.attrs().get(number));
    assertEquals("left", newResult.attrs().get(direction));
    assertEquals("red", newResult.attrs().get(color));
  }

  @Test
  public void testResult_OverrideExistingValueWhenAddingMultipleAttributes() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Result newResult =
        result
            .addAttr(color, "red")
            .addAttrs(new TypedEntry<>(number, 5L), new TypedEntry<>(color, "white"));

    assertTrue(newResult.attrs().containsKey(number));
    assertTrue(newResult.attrs().containsKey(color));
    assertEquals(((Long) 5L), newResult.attrs().get(number));
    assertEquals("white", newResult.attrs().get(color));
  }
}
