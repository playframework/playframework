/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementations to work with URL paths.  This is a utility class with usages by {@link play.mvc.Call}.
 */
public final class Paths {
    private Paths() {}

    private static String CURRENT_DIR = ".";
    private static String SEPARATOR = "/";
    private static String PARENT_DIR = "..";

    /**
     * Create a path to targetPath that's relative to the given startPath.
     */
    public static String relative(String startPath, String targetPath) {
        // If the start and target path's are the same then link to the current directory
        if (startPath.equals(targetPath)) {
            return CURRENT_DIR;
        }

        String[] start = toSegments(canonical(startPath));
        String[] target = toSegments(canonical(targetPath));

        // If start path has no trailing separator (a "file" path), then drop file segment
        if (!startPath.endsWith(SEPARATOR))
            start = Arrays.copyOfRange(start, 0, start.length - 1);

        // If target path has no trailing separator, then drop file segment, but keep a reference to add it later
        String targetFile = "";
        if (!targetPath.endsWith(SEPARATOR)) {
            targetFile = target[target.length-1];
            target = Arrays.copyOfRange(target, 0, target.length - 1);
        }

        // Work out how much of the filepath is shared by start and path.
        String[] common = commonPrefix(start, target);
        String[] parents = toParentDirs(start.length - common.length);

        int relativeStartIdx = common.length;
        String[] relativeDirs = Arrays.copyOfRange(target, relativeStartIdx, target.length);
        String[] relativePath = Arrays.copyOf(parents, parents.length + relativeDirs.length);
        System.arraycopy(relativeDirs, 0, relativePath, parents.length, relativeDirs.length);

        // If this is not a sibling reference append a trailing / to path
        String trailingSep = "";
        if (relativePath.length > 0)
            trailingSep = SEPARATOR;

        return Arrays.stream(relativePath).collect(Collectors.joining(SEPARATOR)) + trailingSep + targetFile;
    }

    /**
     * Create a canonical path that does not contain parent directories, current directories, or superfluous directory
     * separators.
     */
    public static String canonical(String url) {
        String[] urlPath = toSegments(url);
        Stack<String> canonical = new Stack<>();
        for (String comp : urlPath) {
            if (comp.isEmpty() || comp.equals(CURRENT_DIR))
                continue;
            if (!comp.equals(PARENT_DIR) || (!canonical.empty() && canonical.peek().equals(PARENT_DIR)))
                canonical.push(comp);
            else
                canonical.pop();
        }

        String prefixSep = url.startsWith(SEPARATOR) ? SEPARATOR : "";
        String trailingSep = url.endsWith(SEPARATOR) ? SEPARATOR : "";

        return prefixSep + canonical.stream().collect(Collectors.joining(SEPARATOR)) + trailingSep;
    }

    private static String[] toSegments(String url) {
        return Arrays
                .stream(url.split(SEPARATOR))
                .filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private static String[] toParentDirs(int count) {
        return IntStream
                .range(0, count)
                .mapToObj(i -> PARENT_DIR).toArray(String[]::new);
    }

    private static String[] commonPrefix(String[] path1, String[] path2) {
        int minLength = path1.length < path2.length ? path1.length : path2.length;

        ArrayList<String> match = new ArrayList<>();
        for (int i = 0; i < minLength; i++)
            if (!path1[i].equals(path2[i]))
                break;
            else
                match.add(path1[i]);

        return match.toArray(new String[0]);
    }
}
