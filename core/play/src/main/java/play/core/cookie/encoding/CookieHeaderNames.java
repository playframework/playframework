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

final class CookieHeaderNames {
  public static final String PATH = "Path";

  public static final String EXPIRES = "Expires";

  public static final String MAX_AGE = "Max-Age";

  public static final String DOMAIN = "Domain";

  public static final String SECURE = "Secure";

  public static final String HTTPONLY = "HTTPOnly";

  public static final String SAMESITE = "SameSite";

  public static final String PARTITIONED = "Partitioned";

  private CookieHeaderNames() {
    // Unused.
  }
}
