/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;

import java.util.Collections;
import java.util.List;

// #java-asset-cache
public class AssetCache {
  public List<String> cspHashes() {
    return Collections.singletonList("sha256-HELLO");
  }
}
// #java-asset-cache
