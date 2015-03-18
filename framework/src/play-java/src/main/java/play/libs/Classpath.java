/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import play.*;

import org.reflections.*;
import org.reflections.util.*;
import org.reflections.scanners.*;

import java.util.Set;

public class Classpath {

	/**
     * Scans the application classloader to retrieve all types within a specific package.
     * <p>
     * This method is useful for some plug-ins, for example the EBean plugin will automatically detect all types
     * within the models package.
     * <p>
     * Note that it is better to specify a very specific package to avoid expensive searches.
     *
     * @param packageName the root package to scan
     * @return a set of types names satisfying the condition
     */
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
     * @param packageName the root package to scan
     * @param annotation annotation class
     * @return a set of types names statifying the condition
     */
    public static Set<Class<?>> getTypesAnnotatedWith(Application app, String packageName, Class<? extends java.lang.annotation.Annotation> annotation) {
        return getReflections(app, packageName).getTypesAnnotatedWith(annotation);
    }

    private static Reflections getReflections(Application app, String packageName) {
        if (app.isTest()) {
            return ReflectionsCache$.MODULE$.getReflections(app.classloader(), packageName);
        } else {
            return new Reflections(
                new ConfigurationBuilder()
                    .addUrls(ClasspathHelper.forPackage(packageName, app.classloader()))
                    .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName + ".")))
                    .setScanners(new TypeElementsScanner(), new TypeAnnotationsScanner()));
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
     * @param packageName the root package to scan
     * @return a set of types names satisfying the condition
     */
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
     * @param packageName the root package to scan
     * @param annotation annotation class
     * @return a set of types names statifying the condition
     */
    public static Set<Class<?>> getTypesAnnotatedWith(Environment env, String packageName, Class<? extends java.lang.annotation.Annotation> annotation) {
        return getReflections(env, packageName).getTypesAnnotatedWith(annotation);
    }

    private static Reflections getReflections(Environment env, String packageName) {
        if (env.isTest()) {
            return ReflectionsCache$.MODULE$.getReflections(env.classLoader(), packageName);
        } else {
            return new Reflections(
                new ConfigurationBuilder()
                    .addUrls(ClasspathHelper.forPackage(packageName, env.classLoader()))
                    .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName + ".")))
                    .setScanners(new TypeElementsScanner(), new TypeAnnotationsScanner()));
        }
    }

}
