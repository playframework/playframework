/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DevServerSettingsTest {
  private static final int defaultHttpPort = 9000;
  private static final String defaultHttpAddress = "0.0.0.0";

  private void check(
      DevServerSettings settings,
      Map<String, String> argsProperties,
      Integer httpPort,
      Integer httpsPort,
      String httpAddress) {
    assertEquals(httpPort, settings.getHttpPort());
    assertEquals(httpsPort, settings.getHttpsPort());
    assertEquals(httpAddress, settings.getHttpAddress());
    assertEquals(argsProperties, settings.getArgsProperties());
  }

  private DevServerSettings devServerSettings(List<String> args) {
    return DevServerSettings.parse(List.of(), args, Map.of(), defaultHttpPort, defaultHttpAddress);
  }

  private DevServerSettings devServerSettings(Map<String, String> devSettings) {
    return DevServerSettings.parse(
        List.of(), List.of(), devSettings, defaultHttpPort, defaultHttpAddress);
  }

  private DevServerSettings devServerSettings(List<String> args, Map<String, String> devSettings) {
    return DevServerSettings.parse(
        List.of(), args, devSettings, defaultHttpPort, defaultHttpAddress);
  }

  @Test
  public void shouldSupportPortArgument() {
    var settings = devServerSettings(List.of("1234"));
    check(settings, Map.of(), 1234, null, defaultHttpAddress);
  }

  @Test
  public void shouldSupportDisabledPortArgument() {
    var settings = devServerSettings(List.of("disabled"));
    check(settings, Map.of(), null, null, defaultHttpAddress);
  }

  @Test
  public void shouldSupportPortPropertyWithSystemProperty() {
    var settings = devServerSettings(List.of("-Dhttp.port=1234"));
    check(settings, Map.of("http.port", "1234"), 1234, null, defaultHttpAddress);
  }

  @Test
  public void shouldSupportPortPropertyWithDevSetting() {
    var settings = devServerSettings(Map.of("play.server.http.port", "1234"));
    check(settings, Map.of(), 1234, null, defaultHttpAddress);
  }

  @Test
  public void shouldSupportOverridingPortPropertyFromDevSettingByTheOneFromCommandLine() {
    var settings =
        devServerSettings(List.of("-Dhttp.port=9876"), Map.of("play.server.http.port", "1234"));
    check(settings, Map.of("http.port", "9876"), 9876, null, defaultHttpAddress);
  }

  @Test
  public void shouldSupportOverridingPortFromFirstNonPropertyArgumentByTheOneSuppliedAsProperty() {
    var settings = devServerSettings(List.of("5555", "-Dhttp.port=9876"));
    check(settings, Map.of("http.port", "9876"), 9876, null, defaultHttpAddress);
  }

  @Test
  public void shouldSupportPortPropertyLongVersionFromCommandLineThatOverridesEverythingElse() {
    var settings =
        devServerSettings(
            List.of("1234", "-Dplay.server.http.port=5555", "-Dhttp.port=9876"),
            Map.of("play.server.http.port", "5678"));
    check(
        settings,
        Map.of("play.server.http.port", "5555", "http.port", "9876"),
        5555,
        null,
        defaultHttpAddress);
  }

  @Test
  public void shouldSupportDisabledPortProperty() {
    var settings = devServerSettings(List.of("-Dhttp.port=disabled"));
    check(settings, Map.of("http.port", "disabled"), null, null, defaultHttpAddress);
  }

  @Test
  public void shouldSupportHttpsPortProperty() {
    var settings = devServerSettings(List.of("-Dhttps.port=4321"));
    check(settings, Map.of("https.port", "4321"), defaultHttpPort, 4321, defaultHttpAddress);
  }

  @Test
  public void shouldSupportHttpsOnly() {
    var settings = devServerSettings(List.of("-Dhttps.port=4321", "disabled"));
    check(settings, Map.of("https.port", "4321"), null, 4321, defaultHttpAddress);
  }

  @Test
  public void shouldSupportHttpsPortPropertyWithDevSetting() {
    var settings = devServerSettings(Map.of("play.server.https.port", "1234"));
    check(settings, Map.of(), defaultHttpPort, 1234, defaultHttpAddress);
  }

  @Test
  public void shouldSupportHttpsDisabled() {
    var settings = devServerSettings(List.of("-Dhttps.port=disabled", "-Dhttp.port=1234"));
    check(
        settings,
        Map.of("https.port", "disabled", "http.port", "1234"),
        1234,
        null,
        defaultHttpAddress);
  }

  @Test
  public void shouldSupportAddressProperty() {
    var settings = devServerSettings(List.of("-Dhttp.address=localhost"));
    check(settings, Map.of("http.address", "localhost"), defaultHttpPort, null, "localhost");
  }

  @Test
  public void shouldSupportAddressPropertyWithDevSetting() {
    var settings = devServerSettings(Map.of("play.server.http.address", "not-default-address"));
    check(settings, Map.of(), defaultHttpPort, null, "not-default-address");
  }

  @Test
  public void shouldSupportAllOptions() {
    var settings =
        devServerSettings(
            List.of(
                "-Dhttp.address=localhost",
                "-Dhttps.port=4321",
                "-Dtest.option=something",
                "1234"));
    check(
        settings,
        Map.of("http.address", "localhost", "https.port", "4321", "test.option", "something"),
        1234,
        4321,
        "localhost");
  }
}
