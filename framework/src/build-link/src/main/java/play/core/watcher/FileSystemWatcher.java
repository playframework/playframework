package play.core.watcher;

public interface FileSystemWatcher {

  int addWatch(String directoryToWatch);
  void removeWatch(int id);
  void reloaded();
  void changed();
  boolean hasChanged();

}
