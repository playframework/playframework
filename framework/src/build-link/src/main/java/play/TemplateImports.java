/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemplateImports {

  public static final List<String> minimalJavaTemplateImports;
  public static final List<String> defaultJavaTemplateImports;
  public static final List<String> defaultScalaTemplateImports;

  private static final List<String> defaultTemplateImports = Collections.unmodifiableList(
    Arrays.asList(
      "models._",
      "controllers._",
      "play.api.i18n._",
      "views.%format%._",
      "play.api.templates.PlayMagic._"
    ));

  static {
    List<String> minimalJavaImports = new ArrayList<String>();
    minimalJavaImports.addAll(defaultTemplateImports);
    minimalJavaImports.add("java.lang._");
    minimalJavaImports.add("java.util._");
    minimalJavaImports.add("scala.collection.JavaConverters._");
    minimalJavaImports.add("play.core.j.PlayMagicForJava._");
    minimalJavaImports.add("play.mvc._");
    minimalJavaImports.add("play.api.data.Field");
    minimalJavaImports.add("play.mvc.Http.Context.Implicit._");
    minimalJavaTemplateImports = Collections.unmodifiableList(minimalJavaImports);

    List<String> defaultJavaImports = new ArrayList<String>();
    defaultJavaImports.addAll(minimalJavaTemplateImports);
    defaultJavaImports.add("play.data._");
    defaultJavaImports.add("play.core.j.PlayFormsMagicForJava._");
    defaultJavaTemplateImports = Collections.unmodifiableList(defaultJavaImports);

    List<String> scalaImports = new ArrayList<String>();
    scalaImports.addAll(defaultTemplateImports);
    scalaImports.add("play.api.mvc._");
    scalaImports.add("play.api.data._");
    defaultScalaTemplateImports = Collections.unmodifiableList(scalaImports);
  }

}
