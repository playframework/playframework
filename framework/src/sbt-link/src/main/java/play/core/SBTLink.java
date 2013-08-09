package play.core;

import java.io.*;
import java.util.*;

/**
 * Interface used by the Play SBT plugin to communicate with an embedded Play
 * server. SBTLink objects are created by the plugin's run command and provided
 * to Play's NettyServer devMode methods.
 *
 * <p>This interface is written in Java and uses only Java types so that
 * communication can work even when the plugin and embedded Play server are
 * built with different versions of Scala.
 */
public interface SBTLink {

	// Will return either:
	// - Throwable -> If something is wrong
	// - ClassLoader -> If the classLoader changed
	// - null -> if nothing changed
	public Object reload();

	// Will return either:
	// - [File, Integer]
	// - [File, null]
	// - null
	public Object[] findSource(String className, Integer line);

	public File projectPath();

	public Object runTask(String name);

	public void forceReload();

	public Map<String,String> settings();

}