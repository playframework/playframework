/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import static play.gradle.PlayExtension.PLAY_EXTENSION_NAME;
import static play.gradle.internal.Utils.isPlayJava;
import static play.gradle.internal.Utils.mainSourceSet;
import static play.gradle.internal.Utils.playExtension;
import static play.gradle.internal.Utils.scalaSourceDirectorySet;
import static play.gradle.internal.Utils.testSourceSet;

import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.scala.ScalaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;
import play.TemplateImports;
import play.gradle.Language;
import play.gradle.PlayExtension;
import play.twirl.gradle.TwirlSourceDirectorySet;

/** A Gradle plugin to develop Play application. */
@Incubating
public class PlayPlugin implements Plugin<Project> {

  public static final String PLAY_GROUP_ID = "org.playframework";

  public static final String DEFAULT_SCALA_VERSION = "2.13";

  @Inject
  public PlayPlugin() {}

  @Override
  public void apply(@NotNull final Project project) {
    createPlayExtension(project);

    project.getPluginManager().apply(ScalaBasePlugin.class);
    project.getPluginManager().apply(ApplicationPlugin.class);

    configurePlayApplicationLayout(project);
    configureTwirlDefaultImports(project);

    project.getPluginManager().apply(PlayRoutesPlugin.class);
    project.getPluginManager().apply(PlayAssetsPlugin.class);
    project.getPluginManager().apply(PlayRunPlugin.class);
    project.getPluginManager().apply(PlayApplicationPlugin.class);

    // Move all Java source directories into Scala sources
    // https://stackoverflow.com/questions/23261075/compiling-scala-before-alongside-java-with-gradle
    configureMainJavaScalaSourceSets(project);
  }

  private void createPlayExtension(final Project project) {
    project.getExtensions().create(PLAY_EXTENSION_NAME, PlayExtension.class);
    playExtension(project).getLang().convention(Language.JAVA);
  }

  @SuppressWarnings("unchecked")
  private void configureTwirlDefaultImports(final Project project) {
    // If Twirl plugin wasn't applied before Play plugin
    if (project.getExtensions().findByName("twirl") == null) return;

    for (SourceSet sourceSet : List.of(mainSourceSet(project), testSourceSet(project))) {
      var twirlSource = sourceSet.getExtensions().findByName("twirl");
      if (twirlSource == null) continue;
      var imports = ((TwirlSourceDirectorySet) twirlSource).getTemplateImports();
      if (imports != null) {
        imports.addAll(
            () -> {
              if (isPlayJava(project)) {
                return TemplateImports.defaultJavaTemplateImports.iterator();
              }
              return TemplateImports.defaultScalaTemplateImports.iterator();
            });
      }
      var annotations = ((TwirlSourceDirectorySet) twirlSource).getConstructorAnnotations();
      if (annotations != null) annotations.add("@javax.inject.Inject()");
    }
  }

  /**
   * See <a
   * href="https://www.playframework.com/documentation/latest/Anatomy#The-Play-application-layout">Anatomy
   * of a Play application</a>.
   */
  private void configurePlayApplicationLayout(final Project project) {
    SourceSet mainSourceSet = mainSourceSet(project);
    SourceSet testSourceSet = testSourceSet(project);
    mainSourceSet.getResources().srcDir("conf");
    scalaSourceDirectorySet(mainSourceSet).srcDir("app");
    scalaSourceDirectorySet(testSourceSet).srcDir("test");

    // If Twirl plugin was applied before Play plugin
    SourceDirectorySet twirlSourceSet =
        (SourceDirectorySet) mainSourceSet.getExtensions().findByName("twirl");
    if (twirlSourceSet != null) twirlSourceSet.srcDir("app");
  }

  private void configureMainJavaScalaSourceSets(final Project project) {
    SourceSet mainSourceSet = mainSourceSet(project);
    scalaSourceDirectorySet(mainSourceSet).srcDirs(mainSourceSet.getJava().getSrcDirs());
    mainSourceSet.getJava().setSrcDirs(List.of());
  }
}
