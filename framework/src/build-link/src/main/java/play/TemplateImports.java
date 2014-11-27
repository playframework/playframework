package play;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TemplateImports {

  public static List<String> defaultJavaTemplateImports;
  public static List<String> defaultScalaTemplateImports;

  private static List<String> defaultTemplateImports = Collections.unmodifiableList(
    Arrays.asList(
      "models._",
      "controllers._",
      "play.api.i18n._",
      "views.%format%._",
      "play.api.templates.PlayMagic._"
    ));

  static {
    List<String> javaImports = new ArrayList();
    javaImports.addAll(defaultTemplateImports);
    javaImports.add("java.lang._");
    javaImports.add("java.util._");
    javaImports.add("scala.collection.JavaConversions._");
    javaImports.add("scala.collection.JavaConverters._");
    javaImports.add("play.core.j.PlayMagicForJava._");
    javaImports.add("play.mvc._");
    javaImports.add("play.data._");
    javaImports.add("play.api.data.Field");
    javaImports.add("play.mvc.Http.Context.Implicit._");
    defaultJavaTemplateImports = Collections.unmodifiableList(javaImports);

    List<String> scalaImports = new ArrayList();
    scalaImports.addAll(defaultTemplateImports);
    scalaImports.add("play.api.mvc._");
    scalaImports.add("play.api.data._");
    defaultScalaTemplateImports = Collections.unmodifiableList(scalaImports);
  }

}
