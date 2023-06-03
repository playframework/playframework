/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import play.libs.typedmap.TypedEntry;
import play.libs.typedmap.TypedKey;
import play.libs.typedmap.TypedMap;

public final class ResultAttributesTest {

  public static Stream<Arguments> targets() {
    return Stream.of(arguments(named("OK", Results.ok())));
  }

  @ParameterizedTest
  @MethodSource("targets")
  public void testResult_emptyByDefault(final Result result) {
    assertEquals(TypedMap.empty(), result.attrs());
  }

  @ParameterizedTest
  @MethodSource("targets")
  public void testResult_addSingleAttribute(final Result result) {
    final TypedKey<String> color = TypedKey.create("color");

    final Result newResult = result.addAttr(color, "red");

    assertTrue(newResult.attrs().containsKey(color));
    assertEquals("red", newResult.attrs().get(color));
  }

  @ParameterizedTest
  @MethodSource("targets")
  public void testResult_KeepCurrentAttributesWhenAddingANewOne(final Result result) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Result newResult = result.addAttr(color, "red").addAttr(number, 5L);

    assertTrue(newResult.attrs().containsKey(number));
    assertTrue(newResult.attrs().containsKey(color));
    assertEquals(((Long) 5L), newResult.attrs().get(number));
    assertEquals("red", newResult.attrs().get(color));
  }

  @ParameterizedTest
  @MethodSource("targets")
  public void testResult_OverrideExistingValue(final Result result) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Result newResult = result.addAttr(color, "red").addAttr(number, 5L).addAttr(color, "white");

    assertTrue(newResult.attrs().containsKey(number));
    assertTrue(newResult.attrs().containsKey(color));
    assertEquals(((Long) 5L), newResult.attrs().get(number));
    assertEquals("white", newResult.attrs().get(color));
  }

  @ParameterizedTest
  @MethodSource("targets")
  public void testResult_addMultipleAttributes(final Result result) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    final Result newResult =
        result.addAttrs(new TypedEntry<>(color, "red"), new TypedEntry<>(number, 3L));

    assertTrue(newResult.attrs().containsKey(color));
    assertTrue(newResult.attrs().containsKey(number));
    assertEquals("red", newResult.attrs().get(color));
    assertEquals((Long) 3L, newResult.attrs().get(number));
  }

  @ParameterizedTest
  @MethodSource("targets")
  public void testResult_KeepCurrentAttributesWhenAddingMultipleOnes(final Result result) {
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

  @ParameterizedTest
  @MethodSource("targets")
  public void testResult_OverrideExistingValueWhenAddingMultipleAttributes(final Result result) {
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
