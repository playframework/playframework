/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package play.core.cookie.encoding;

import static play.core.cookie.encoding.CookieUtil.*;

/** Parent of Client and Server side cookie encoders */
abstract class CookieEncoder {

  private final boolean strict;

  protected CookieEncoder(boolean strict) {
    this.strict = strict;
  }

  protected void validateCookie(String name, String value) {
    if (strict) {
      int pos;

      if ((pos = firstInvalidCookieNameOctet(name)) >= 0) {
        throw new IllegalArgumentException(
            "Cookie (" + name + ") contains an invalid char: " + name.charAt(pos));
      }

      CharSequence unwrappedValue = unwrapValue(value);
      if (unwrappedValue == null) {
        throw new IllegalArgumentException(
            "Cookie (" + name + ") value wrapping quotes are not balanced: " + value);
      }

      if ((pos = firstInvalidCookieValueOctet(unwrappedValue)) >= 0) {
        throw new IllegalArgumentException(
            "Cookie (" + name + ") value contains an invalid char: " + value.charAt(pos));
      }
    }
  }
}
