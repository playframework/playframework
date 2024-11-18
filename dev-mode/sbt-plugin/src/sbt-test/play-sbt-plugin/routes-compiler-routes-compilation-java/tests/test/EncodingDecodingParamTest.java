/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.HttpVerbs.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

public class EncodingDecodingParamTest extends AbstractRoutesTest {

  private void checkDecoding(
      String dynamicEncoded,
      String staticEncoded,
      String queryEncoded,
      String dynamicDecoded,
      String staticDecoded,
      String queryDecoded) {
    var path = String.format("/urlcoding/%s/%s?q=%s", dynamicEncoded, staticEncoded, queryEncoded);
    var expected =
        String.format("dynamic=%s static=%s query=%s", dynamicDecoded, staticDecoded, queryDecoded);
    var result = route(app, fakeRequest(GET, path));
    assertThat(contentAsString(result)).isEqualTo(expected);
  }

  private void checkEncoding(
      String dynamicDecoded,
      String staticDecoded,
      String queryDecoded,
      String dynamicEncoded,
      String staticEncoded,
      String queryEncoded) {
    var expected =
        String.format("/urlcoding/%s/%s?q=%s", dynamicEncoded, staticEncoded, queryEncoded);
    var call =
        controllers.routes.Application.urlcoding(dynamicDecoded, staticDecoded, queryDecoded);
    assertThat(call.url()).isEqualTo(expected);
  }

  @Test
  public void checkDecoding() {
    checkDecoding("123", "456", "789", "123", "456", "789");
    checkDecoding("a", "a", "a", "a", "a", "a");
    checkDecoding("%2B", "%2B", "%2B", "+", "%2B", "+");
    checkDecoding("+", "+", "+", "+", "+", " ");
    checkDecoding("%20", "%20", "%20", " ", "%20", " ");
    checkDecoding("&", "&", "-", "&", "&", "-");
    checkDecoding("=", "=", "-", "=", "=", "-");
  }

  @Test
  public void checkEncoding() {
    checkEncoding("123", "456", "789", "123", "456", "789");
    checkEncoding("+", "+", "+", "+", "+", "%2B");
    checkEncoding(" ", " ", " ", "%20", " ", "+");
    checkEncoding("&", "&", "&", "&", "&", "%26");
    checkEncoding("=", "=", "=", "=", "=", "%3D");
    // We use java.net.URLEncoder for query string encoding, which is not
    // RFC compliant, e.g. it percent-encodes "/" which is not a delimiter
    // for query strings, and it percent-encodes "~" which is an "unreserved" character
    // that should never be percent-encoded. The following tests, therefore
    // don't really capture our ideal desired behaviour for query string
    // encoding. However, the behaviour for dynamic and static paths is correct.
    checkEncoding("/", "/", "/", "%2F", "/", "%2F");
    checkEncoding("~", "~", "~", "~", "~", "%7E");
  }
}
