package play.core.watcher;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * USING jnotify to detect file change
 * This creates a fully dynamic version of JNotify that support reloading 
 * TODO: Use java.nio.file.WatchService in place of jnotify if Java 7 is available
 */
public class FileSystemWatcherFactory {

  private final File nativeLibrariesDirectory;

  public FileSystemWatcherFactory(File nativeLibrariesDirectory) {
    this.nativeLibrariesDirectory = nativeLibrariesDirectory;
  }

  public FileSystemWatcher createWatcher()
      throws IllegalAccessException, NoSuchMethodException, MalformedURLException, NoSuchFieldException, ClassNotFoundException, InvocationTargetException {

  	// Use an array to make final and mutable for access from inner class
    final boolean[] _changed = new boolean[] { true };

    URLClassLoader buildLoader = (URLClassLoader) this.getClass().getClassLoader().getParent();

    String libs = new File(nativeLibrariesDirectory, System.getProperty("sun.arch.data.model") + "bits").getAbsolutePath();

    // Hack to set java.library.path
    String path = System.getProperty("java.library.path") == null
        ? libs
        : System.getProperty("java.library.path") + File.pathSeparator + libs;
    System.setProperty("java.library.path", path);
    Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
    fieldSysPath.setAccessible(true);
    fieldSysPath.set(null, null);

    Class<?> jnotifyClass = buildLoader.loadClass("net.contentobjects.jnotify.JNotify");
    Class<?> jnotifyListenerClass = buildLoader.loadClass("net.contentobjects.jnotify.JNotifyListener");
    final Method addWatchMethod = jnotifyClass.getMethod("addWatch", String.class, int.class, boolean.class, jnotifyListenerClass);
    final Method removeWatchMethod = jnotifyClass.getMethod("removeWatch", int.class);
    final Object listener = Proxy.newProxyInstance(buildLoader, new Class<?>[] { jnotifyListenerClass }, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method m, Object[] args) {
        _changed[0] = true;
        return null;
      }
    });

    FileSystemWatcher nativeWatcher = new FileSystemWatcher() {
      @Override
      public int addWatch(String directoryToWatch) {
      	try {
          return (Integer) addWatchMethod.invoke(null, directoryToWatch, 15, true, listener);
        } catch (IllegalAccessException e) {
          throw new IllegalStateException(e);
        } catch(InvocationTargetException e) {
          throw new IllegalStateException(e);
        }
      }
      @Override
      public void removeWatch(int id) {
      	try {
      	  removeWatchMethod.invoke(null, (Object) id);
      	} catch (IllegalAccessException e) {
          throw new IllegalStateException(e);
        } catch(InvocationTargetException e) {
          throw new IllegalStateException(e);
        }
      }
      @Override
      public void reloaded() { _changed[0] = false; }
      @Override
      public void changed() { _changed[0] = true; }
      @Override
      public boolean hasChanged() { return _changed[0]; }
    };

    return nativeWatcher;
  }

  public static FileSystemWatcher createFakeWatcher() {
    return new FileSystemWatcher() {
      @Override
      public int addWatch(String directoryToWatch) { return 0; }
      @Override
      public void removeWatch(int id) { }
      @Override
      public void reloaded() { }
      @Override
      public void changed() { }
      @Override
      public boolean hasChanged() { return true; }
    };
  }

}
