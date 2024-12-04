/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

public class MultiParamTest extends AbstractRoutesTest {

  @Test
  public void checkMultiplyParams() {
    var result =
        route(
            app,
            fakeRequest(
                GET,
                "/multi-params?a=a&b=b&c=c&d=d&e=e&f=f&g=g&h=h&i=i&j=j&k=k&l=l&m=m&n=n&o=o&p=p&q=q&r=r&s=s&t=t&u=u&v=v&w=w&x=x&y=y&z=z"));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).isEqualTo("abcdefghijklmnopqrstuvwxyz");
  }
}
