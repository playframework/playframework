/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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