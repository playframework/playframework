/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
package play.gradle.plugin;

import static java.util.stream.Collectors.joining;
import static org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE;
import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;
import static play.gradle.internal.Utils.getDefaultPlayVersion;
import static play.gradle.internal.Utils.isGradleVersionLessThan;
import static play.gradle.internal.Utils.isPlayJava;
import static play.gradle.internal.Utils.javaPluginExtension;
import static play.gradle.internal.Utils.mainSourceSet;
import static play.gradle.internal.Utils.playExtension;
import static play.gradle.internal.Utils.scalaSourceDirectorySet;
import static play.gradle.plugin.PlayPlugin.DEFAULT_SCALA_VERSION;
import static play.gradle.plugin.PlayPlugin.PLAY_GROUP_ID;

import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import play.gradle.PlayExtension;
import play.gradle.task.RoutesCompile;

/** A Gradle plugin to compile Play Routes. */
@Incubating
public class PlayRoutesPlugin implements Plugin<Project> {

  public static final String ROUTES_SOURCE_NAME = "routes";

  public static final String PLAY_ROUTES_USAGE = "play-routes";

  public static final Attribute<String> SOURCE_SET_ATTRIBUTE =
      Attribute.of("org.playframework.routes.sourceset", String.class);

  public static final Attribute<String> AGGREGATED_PROJECTS_ATTRIBUTE =
      Attribute.of("org.playframework.routes.aggregated", String.class);

  private static final List<String> ROUTES_SOURCE_INCLUDES = List.of("routes", "*.routes");

  private static final List<String> DEFAULT_SCALA_IMPORTS = List.of("controllers.Assets.Asset");

  private static final List<String> DEFAULT_JAVA_IMPORTS =
      List.of("play.libs.F", "controllers.Assets.Asset");

  private final ObjectFactory objectFactory;

  @Inject
  public PlayRoutesPlugin(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public void apply(@NotNull final Project project) {
    configureDefaultSettings(project);
    Configuration routesConfiguration = createDefaultRoutesConfiguration(project);
    configureSourceSetDefaults(project, routesConfiguration);
  }

  private Provider<Boolean> shouldGenerateReverseRouter(final Project project) {
    Configuration graph = createRoutesGraphConfiguration(project);
    String projectPath = project.getPath();
    return project.provider(
        () -> !isAggregatedBy(graph.getIncoming().getResolutionResult(), projectPath));
  }

  private static boolean isAggregatedBy(ResolutionResult dependencies, String projectPath) {
    return dependencies.getAllDependencies().stream()
        .filter(ResolvedDependencyResult.class::isInstance)
        .map(dependency -> ((ResolvedDependencyResult) dependency).getResolvedVariant())
        .map(variant -> variant.getAttributes().getAttribute(AGGREGATED_PROJECTS_ATTRIBUTE))
        .filter(Objects::nonNull)
        .anyMatch(aggregated -> List.of(aggregated.split(",")).contains(projectPath));
  }

  private Configuration createRoutesGraphConfiguration(final Project project) {
    SourceSet mainSourceSet = mainSourceSet(project);
    Configuration conf = project.getConfigurations().create("playRoutesGraph");
    conf.setDescription("The dependency graph used to detect aggregation of the reverse routes.");
    conf.setVisible(false);
    conf.setCanBeConsumed(false);
    conf.setCanBeResolved(true);
    conf.extendsFrom(
        project.getConfigurations().getByName(mainSourceSet.getImplementationConfigurationName()),
        project.getConfigurations().getByName(mainSourceSet.getRuntimeOnlyConfigurationName()));
    conf.getAttributes().attribute(SOURCE_SET_ATTRIBUTE, mainSourceSet.getName());
    return conf;
  }

  private void configureDefaultSettings(final Project project) {
    var routes = playExtension(project).getRoutes();
    routes.getNamespaceReverseRouter().convention(false);
    routes.getAggregateReverseRoutes().convention(project.getObjects().listProperty(Project.class));
    Provider<Boolean> generateReverseRouter = shouldGenerateReverseRouter(project);
    routes.getGenerateReverseRouter().convention(generateReverseRouter);
    routes.getGenerateJsReverseRouter().convention(generateReverseRouter);
    routes
        .getImports()
        .addAll(
            () -> {
              if (isPlayJava(project)) {
                return DEFAULT_JAVA_IMPORTS.iterator();
              }
              return DEFAULT_SCALA_IMPORTS.iterator();
            });
  }

  private Configuration createDefaultRoutesConfiguration(final Project project) {
    Configuration conf = project.getConfigurations().create("playRoutes");
    conf.setDescription("The Play Routes compiler library.");
    conf.setVisible(false);
    conf.setTransitive(true);
    conf.setCanBeConsumed(false);
    conf.setCanBeResolved(true);
    conf.defaultDependencies(
        dependencies -> {
          Dependency playRoutesCompiler =
              project
                  .getDependencies()
                  .create(
                      String.format(
                          "%s:play-routes-compiler_%s:%s",
                          PLAY_GROUP_ID, DEFAULT_SCALA_VERSION, getDefaultPlayVersion()));
          dependencies.add(playRoutesCompiler);
        });
    return conf;
  }

  private void configureSourceSetDefaults(
      final Project project, final Configuration routesConfiguration) {
    javaPluginExtension(project)
        .getSourceSets()
        .all(
            (sourceSet) -> {
              SourceDirectorySet routesSource = createRoutesSourceDirectorySet(sourceSet);
              createRoutesElementsConfiguration(project, sourceSet, routesSource);
              Configuration aggregateRoutes =
                  createAggregateRoutesConfiguration(project, sourceSet);
              TaskProvider<RoutesCompile> routesCompileTask =
                  createRoutesCompileTask(
                      project, sourceSet, routesSource, routesConfiguration, aggregateRoutes);

              scalaSourceDirectorySet(sourceSet).srcDir(routesCompileTask);
            });
  }

  private void createRoutesElementsConfiguration(
      final Project project, final SourceSet sourceSet, final SourceDirectorySet routesSource) {
    Configuration conf =
        project.getConfigurations().create(sourceSet.getTaskName(null, "playRoutesElements"));
    conf.setDescription("Routes sources of the " + sourceSet.getName() + " source set.");
    conf.setVisible(false);
    conf.setCanBeConsumed(true);
    conf.setCanBeResolved(false);
    conf.getAttributes()
        .attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage.class, PLAY_ROUTES_USAGE))
        .attribute(CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, Category.LIBRARY))
        .attribute(SOURCE_SET_ATTRIBUTE, sourceSet.getName());
    conf.extendsFrom(
        project.getConfigurations().getByName(sourceSet.getImplementationConfigurationName()),
        project.getConfigurations().getByName(sourceSet.getRuntimeOnlyConfigurationName()));
    declareAggregatedProjects(project, conf);
    // Source directories can still be added while the build script is evaluated
    if (isGradleVersionLessThan("7.4")) {
      // Publishing a lazily computed set of artifacts is not supported in Gradle < 7.4
      project.afterEvaluate(
          evaluated ->
              routesSource.getSrcDirs().forEach(srcDir -> conf.getOutgoing().artifact(srcDir)));
    } else {
      conf.getOutgoing().artifacts(project.provider(routesSource::getSrcDirs));
    }
  }

  private void declareAggregatedProjects(final Project project, final Configuration conf) {
    // The aggregation is only known once the build script has configured the extension
    Provider<String> aggregatedProjects = aggregatedProjectPaths(project);
    if (isGradleVersionLessThan("7.4")) {
      // Declaring a lazily computed attribute is not supported yet
      project.afterEvaluate(
          evaluated ->
              conf.getAttributes()
                  .attribute(AGGREGATED_PROJECTS_ATTRIBUTE, aggregatedProjects.get()));
    } else {
      conf.getAttributes().attributeProvider(AGGREGATED_PROJECTS_ATTRIBUTE, aggregatedProjects);
    }
  }

  private Provider<String> aggregatedProjectPaths(final Project project) {
    return playExtension(project)
        .getRoutes()
        .getAggregateReverseRoutes()
        .map(projects -> projects.stream().map(Project::getPath).collect(joining(",")));
  }

  private Configuration createAggregateRoutesConfiguration(
      final Project project, final SourceSet sourceSet) {
    Configuration conf =
        project.getConfigurations().create(sourceSet.getTaskName(null, "playAggregateRoutes"));
    conf.setDescription(
        "Routes sources aggregated into the " + sourceSet.getName() + " source set.");
    conf.setVisible(false);
    conf.setCanBeConsumed(false);
    conf.setCanBeResolved(true);
    conf.setTransitive(false);
    conf.getAttributes()
        .attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage.class, PLAY_ROUTES_USAGE))
        .attribute(CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, Category.LIBRARY))
        .attribute(SOURCE_SET_ATTRIBUTE, sourceSet.getName());
    conf.withDependencies(
        dependencies ->
            playExtension(project)
                .getRoutes()
                .getAggregateReverseRoutes()
                .get()
                .forEach(
                    aggregated -> dependencies.add(project.getDependencies().create(aggregated))));
    return conf;
  }

  private TaskProvider<RoutesCompile> createRoutesCompileTask(
      final Project project,
      final SourceSet sourceSet,
      SourceDirectorySet routesSource,
      final Configuration routesConfiguration,
      final Configuration aggregateRoutes) {
    return project
        .getTasks()
        .register(
            sourceSet.getCompileTaskName("playRoutes"),
            RoutesCompile.class,
            routesCompile -> {
              PlayExtension playExtension = playExtension(project);
              routesCompile.setDescription("Compiles the " + routesSource + ".");
              routesCompile.getRoutesCompilerClasspath().setFrom(routesConfiguration);
              routesCompile.getSource().setFrom(routesSource);
              routesCompile
                  .getAggregateSource()
                  .setFrom(
                      aggregateRoutes
                          .getIncoming()
                          .getFiles()
                          .getAsFileTree()
                          .matching(pattern -> pattern.include(ROUTES_SOURCE_INCLUDES)));
              routesCompile
                  .getGenerateReverseRouter()
                  .convention(playExtension.getRoutes().getGenerateReverseRouter());
              routesCompile
                  .getGenerateJsReverseRouter()
                  .convention(playExtension.getRoutes().getGenerateJsReverseRouter());
              routesCompile
                  .getNamespaceReverseRouter()
                  .convention(playExtension.getRoutes().getNamespaceReverseRouter());
              routesCompile.getImports().convention(playExtension.getRoutes().getImports());
              DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();
              routesCompile
                  .getDestinationDirectory()
                  .convention(
                      buildDirectory.dir(
                          "generated/sources/play/"
                              + ROUTES_SOURCE_NAME
                              + "/"
                              + sourceSet.getName()));
            });
  }

  private SourceDirectorySet createRoutesSourceDirectorySet(SourceSet sourceSet) {
    String displayName = ((DefaultSourceSet) sourceSet).getDisplayName();
    SourceDirectorySet routesSource =
        objectFactory.sourceDirectorySet(ROUTES_SOURCE_NAME, displayName + " Play Routes source");
    routesSource.setSrcDirs(sourceSet.getResources().getSrcDirs());
    routesSource.include(ROUTES_SOURCE_INCLUDES);
    sourceSet.getExtensions().add(SourceDirectorySet.class, ROUTES_SOURCE_NAME, routesSource);
    return routesSource;
  }
}
