/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.runsupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DevServerSettings {

  public static final String PORT_DISABLED = "disabled";

  private static final Pattern SYSTEM_PROPERTY = Pattern.compile("-D([^=]+)=(.*)");

  private final LinkedHashMap<String, String> javaOptionProperties;
  private final LinkedHashMap<String, String> argsProperties;
  private final Integer httpPort;
  private final Integer httpsPort;
  private final String httpAddress;

  private DevServerSettings(
      LinkedHashMap<String, String> javaOptionProperties,
      LinkedHashMap<String, String> argsProperties,
      Integer httpPort,
      Integer httpsPort,
      String httpAddress) {
    this.javaOptionProperties = javaOptionProperties;
    this.argsProperties = argsProperties;
    this.httpPort = httpPort;
    this.httpsPort = httpsPort;
    this.httpAddress = httpAddress;
  }

  public String getHttpPortOrDisabled() {
    return httpPort != null ? httpPort.toString() : PORT_DISABLED;
  }

  public boolean isAnyPortDefined() {
    return isHttpPortDefined() || isHttpsPortDefined();
  }

  public boolean isHttpPortDefined() {
    return httpPort != null;
  }

  public boolean isHttpsPortDefined() {
    return httpsPort != null;
  }

  public LinkedHashMap<String, String> getJavaOptionProperties() {
    return javaOptionProperties;
  }

  public LinkedHashMap<String, String> getArgsProperties() {
    return argsProperties;
  }

  public LinkedHashMap<String, String> getSystemProperties() {
    // Properties are combined in this specific order so that command line
    // properties win over the configured one, making them more useful.
    var systemProperties = new LinkedHashMap<String, String>();
    systemProperties.putAll(javaOptionProperties);
    systemProperties.putAll(argsProperties);
    systemProperties.put("play.server.http.address", httpAddress);
    if (isHttpPortDefined()) {
      systemProperties.put("play.server.http.port", httpPort.toString());
    }
    if (isHttpsPortDefined()) {
      systemProperties.put("play.server.https.port", httpsPort.toString());
    }
    return systemProperties;
  }

  public Integer getHttpPort() {
    return httpPort;
  }

  public Integer getHttpsPort() {
    return httpsPort;
  }

  public String getHttpAddress() {
    return httpAddress;
  }

  public static DevServerSettings parse(
      List<String> javaOptions,
      List<String> args,
      Map<String, String> devSettings,
      int defaultHttpPort,
      String defaultHttpAddress) {
    var propertyArgs = extractProperties(args);
    var otherFirstArg = args.stream().filter(__ -> !__.startsWith("-D")).findFirst();

    Function<String, Optional<String>> findInArgsOrSystemProps =
        (String key) ->
            Optional.ofNullable(propertyArgs.get(key))
                .or(() -> Optional.ofNullable(System.getProperty(key)));

    // http port can be defined as the first non-property argument, or a -D(play.server.)http.port
    // argument or system property
    // the http port can be disabled (set to None) by setting any of the input methods to "disabled"
    // Or it can be defined in settings as "play.server.http.port"
    var httpPortString =
        findInArgsOrSystemProps
            .apply("play.server.http.port")
            .or(() -> findInArgsOrSystemProps.apply("http.port"))
            .or(() -> otherFirstArg)
            .or(() -> Optional.ofNullable(devSettings.get("play.server.http.port")))
            .or(() -> Optional.ofNullable(System.getenv("PLAY_HTTP_PORT")))
            .orElse(null);
    var httpPort = parsePort(httpPortString, defaultHttpPort);

    // https port can be defined as a -D(play.server.)https.port argument or system property
    var httpsPortString =
        findInArgsOrSystemProps
            .apply("play.server.https.port")
            .or(() -> findInArgsOrSystemProps.apply("https.port"))
            .or(() -> Optional.ofNullable(devSettings.get("play.server.https.port")))
            .or(() -> Optional.ofNullable(System.getenv("PLAY_HTTPS_PORT")))
            .orElse(null);
    var httpsPort = parsePort(httpsPortString, null);

    // http address can be defined as a -D(play.server.)http.address argument or system property
    var httpAddress =
        findInArgsOrSystemProps
            .apply("play.server.http.address")
            .or(() -> findInArgsOrSystemProps.apply("http.address"))
            .or(() -> Optional.ofNullable(devSettings.get("play.server.http.address")))
            .or(() -> Optional.ofNullable(System.getenv("PLAY_HTTP_ADDRESS")))
            .orElse(defaultHttpAddress);

    return new DevServerSettings(
        extractProperties(javaOptions), propertyArgs, httpPort, httpsPort, httpAddress);
  }

  private static Integer parsePort(String portValue, Integer defaultValue) {
    if (portValue == null) return defaultValue;
    if (portValue.equalsIgnoreCase(PORT_DISABLED)) return null;
    try {
      return Integer.parseInt(portValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid port argument: " + portValue);
    }
  }

  /** Take all the options of the format "-Dfoo=bar" and return them as a key value pairs */
  private static LinkedHashMap<String, String> extractProperties(List<String> args) {
    return args.stream()
        .map(SYSTEM_PROPERTY::matcher)
        .filter(Matcher::matches)
        .collect(
            Collectors.toMap(
                m -> m.group(1),
                m -> m.group(2),
                (existing, newValue) -> newValue, // latest value wins
                LinkedHashMap::new));
  }
}
