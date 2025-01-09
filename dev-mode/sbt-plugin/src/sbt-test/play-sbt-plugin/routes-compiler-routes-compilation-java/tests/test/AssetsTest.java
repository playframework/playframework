/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package test;

import controllers.Assets;
import org.junit.Test;

import static controllers.routes.Assets;
import static org.assertj.core.api.Assertions.assertThat;

public class AssetsTest {

  @Test
  public void checkFingerprintAssets() {
    assertThat(Assets.versioned(new Assets.Asset("css/main.css")).url())
        .isEqualTo("/public/css/abcd1234-main.css");
  }

  @Test
  public void checkSelectedMinifiedVersion() {
    assertThat(Assets.versioned(new Assets.Asset("css/minmain.css")).url())
        .isEqualTo("/public/css/abcd1234-minmain-min.css");
  }

  @Test
  public void checkWorkForNonFingerprintedAssets() {
    assertThat(Assets.versioned(new Assets.Asset("css/nonfingerprinted.css")).url())
        .isEqualTo("/public/css/nonfingerprinted.css");
  }

  @Test
  public void checkSelectedTheMinifiedNonFingerprintedVersion() {
    assertThat(Assets.versioned(new Assets.Asset("css/nonfingerprinted-minmain.css")).url())
        .isEqualTo("/public/css/nonfingerprinted-minmain-min.css");
  }
}
