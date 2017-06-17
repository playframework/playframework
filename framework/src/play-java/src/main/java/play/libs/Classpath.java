/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;


import play.Application;
import play.Environment;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Set;

/**
 * Set of utilities for classpath manipulation.  This class should not be used, as
 * it was part of the Plugin API system which no longer exists in Play.
 *
 * @deprecated Deprecated as of 2.6.0
 */
@Deprecated
public class Classpath {

    /**
     * Scans the application classloader to retrieve all types within a specific package.
     * <p>
     * This method is useful for some plug-ins, for example the EBean plugin will automatically detect all types
     * within the models package.
     * <p>
     * Note that it is better to specify a very specific package to avoid expensive searches.
     *
     * @deprecated Deprecated as of 2.6.0
     * @param app         the Play application
     * @param packageName the root package to scan
     * @return a set of types names satisfying the condition
     */
    @Deprecated
    public static Set<String> getTypes(Application app, String packageName) {
        return getReflections(app, packageName).getStore().get(TypeElementsScanner.class.getSimpleName()).keySet();
    }

    /**
     * Scans the application classloader to retrieve all types annotated with a specific annotation.
     * <p>
     * This method is useful for some plug-ins, for example the EBean plugin will automatically detect all types
     * annotated with <code>@javax.persistance.Entity</code>.
     * <p>
     * Note that it is better to specify a very specific package to avoid expensive searches.
     *
     * @deprecated Deprecated as of 2.6.0
     * @param app         the play application.
     * @param packageName the root package to scan
     * @param annotation annotation class
     * @return a set of types names statifying the condition
     */
    @Deprecated
    public static Set<Class<?>> getTypesAnnotatedWith(Application app, String packageName, Class<? extends java.lang.annotation.Annotation> annotation) {
        return getReflections(app, packageName).getTypesAnnotatedWith(annotation);
    }

    private static Reflections getReflections(Application app, String packageName) {
        if (app.isTest()) {
            return ReflectionsCache$.MODULE$.getReflections(app.classloader(), packageName);
        } else {
            return new Reflections(getReflectionsConfiguration(packageName, app.classloader()));
        }
    }

    /**
     * Scans the environment classloader to retrieve all types within a specific package.
     * <p>
     * This method is useful for some plug-ins, for example the EBean plugin will automatically detect all types
     * within the models package.
     * <p>
     * Note that it is better to specify a very specific package to avoid expensive searches.
     *
     * @deprecated Deprecated as of 2.6.0
     * @param env         the Play environment.
     * @param packageName the root package to scan
     * @return a set of types names satisfying the condition
     */
    @Deprecated
    public static Set<String> getTypes(Environment env, String packageName) {
        return getReflections(env, packageName).getStore().get(TypeElementsScanner.class.getSimpleName()).keySet();
    }

    /**
     * Scans the environment classloader to retrieve all types annotated with a specific annotation.
     * <p>
     * This method is useful for some plug-ins, for example the EBean plugin will automatically detect all types
     * annotated with <code>@javax.persistance.Entity</code>.
     * <p>
     * Note that it is better to specify a very specific package to avoid expensive searches.
     *
     * @deprecated Deprecated as of 2.6.0
     * @param env         the Play environment.
     * @param packageName the root package to scan
     * @param annotation annotation class
     * @return a set of types names statifying the condition
     */
    @Deprecated
    public static Set<Class<?>> getTypesAnnotatedWith(Environment env, String packageName, Class<? extends java.lang.annotation.Annotation> annotation) {
        return getReflections(env, packageName).getTypesAnnotatedWith(annotation);
    }

    private static Reflections getReflections(Environment env, String packageName) {
        if (env.isTest()) {
            return ReflectionsCache$.MODULE$.getReflections(env.classLoader(), packageName);
        } else {
            return new Reflections(getReflectionsConfiguration(packageName, env.classLoader()));
        }
    }

    /**
     * Create {@link org.reflections.Configuration} object for given package name and class loader.
     *
     * @deprecated Deprecated as of 2.6.0
     * @param packageName the root package to scan
     * @param classLoader class loader to be used in reflections
     * @return the configuration builder
     */
    @Deprecated
    public static ConfigurationBuilder getReflectionsConfiguration(String packageName, ClassLoader classLoader) {
        return new ConfigurationBuilder()
            .addUrls(ClasspathHelper.forPackage(packageName, classLoader))
            .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName + ".")))
            .setScanners(new TypeElementsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner());
    }

}
