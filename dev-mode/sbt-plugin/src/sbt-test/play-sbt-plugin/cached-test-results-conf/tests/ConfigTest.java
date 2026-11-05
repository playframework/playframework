/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import static org.junit.Assert.assertEquals;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

public class ConfigTest {

  @Test
  public void readsApplicationConf() {
    assertEquals("hello", ConfigFactory.load().getString("greeting"));
  }
}
