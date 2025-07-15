/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import play.core.j.RequestHeaderImpl;
import play.libs.typedmap.TypedEntry;
import play.libs.typedmap.TypedKey;
import play.libs.typedmap.TypedMap;

@RunWith(Parameterized.class)
public final class AttributesTest {

  @Parameters
  public static Collection<Http.RequestHeader> targets() {
    return Arrays.asList(
        new Http.RequestBuilder().build(),
        new RequestHeaderImpl(new Http.RequestBuilder().build().asScala()));
  }

  private Http.RequestHeader requestHeader;

  public AttributesTest(final Http.RequestHeader requestHeader) {
    this.requestHeader = requestHeader;
  }

  @Test
  public void testRequestHeader_addSingleAttribute() {
    final TypedKey<String> color = TypedKey.create("color");

    final Http.RequestHeader newRequestHeader = requestHeader.addAttr(color, "red");

    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals("red", newRequestHeader.attrs().get(color));
  }

  @Test
  public void testRequestHeader_KeepCurrentAttributesWhenAddingANewOne() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Http.RequestHeader newRequestHeader = requestHeader.addAttr(color, "red").addAttr(number, 5L);

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("red", newRequestHeader.attrs().get(color));
  }

  @Test
  public void testRequestHeader_OverrideExistingValue() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Http.RequestHeader newRequestHeader =
        requestHeader.addAttr(color, "red").addAttr(number, 5L).addAttr(color, "white");

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("white", newRequestHeader.attrs().get(color));
  }

  @Test
  public void testRequestHeader_addMultipleAttributes() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    final Http.RequestHeader newRequestHeader =
        requestHeader.addAttrs(new TypedEntry<>(color, "red"), new TypedEntry<>(number, 3L));

    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertEquals("red", newRequestHeader.attrs().get(color));
    assertEquals((Long) 3L, newRequestHeader.attrs().get(number));
  }

  @Test
  public void testRequestHeader_KeepCurrentAttributesWhenAddingMultipleOnes() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");
    final TypedKey<String> direction = TypedKey.create("direction");

    Http.RequestHeader newRequestHeader =
        requestHeader
            .addAttr(color, "red")
            .addAttrs(new TypedEntry<>(number, 5L), new TypedEntry<>(direction, "left"));

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(direction));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("left", newRequestHeader.attrs().get(direction));
    assertEquals("red", newRequestHeader.attrs().get(color));
  }

  @Test
  public void testRequestHeader_OverrideExistingValueWhenAddingMultipleAttributes() {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Http.RequestHeader newRequestHeader =
        requestHeader
            .addAttr(color, "red")
            .addAttrs(new TypedEntry<>(number, 5L), new TypedEntry<>(color, "white"));

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("white", newRequestHeader.attrs().get(color));
  }

  @Test
  public void testRequestHeader_emptyAttributesCookies() {
    Http.RequestHeader newRequestHeader = requestHeader.withAttrs(TypedMap.empty());
    assertFalse(newRequestHeader.cookies().iterator().hasNext());
  }

  @Test
  public void testRequestHeader_emptyAttributesSession() {
    Http.RequestHeader newRequestHeader = requestHeader.withAttrs(TypedMap.empty());
    assertTrue(newRequestHeader.session().data().isEmpty());
    assertTrue(newRequestHeader.session().asScala().isEmpty());
  }

  @Test
  public void testRequestHeader_emptyAttributesFlash() {
    Http.RequestHeader newRequestHeader = requestHeader.withAttrs(TypedMap.empty());
    assertTrue(newRequestHeader.flash().data().isEmpty());
    assertTrue(newRequestHeader.flash().asScala().isEmpty());
  }
}
