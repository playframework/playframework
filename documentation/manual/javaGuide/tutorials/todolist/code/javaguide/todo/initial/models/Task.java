//#model-base-task
//###replace: package models;
package javaguide.todo.initial.models;

import java.util.*;

public class Task {
    
  public Long id;
  public String label;
  
  public static List<Task> all() {
    return new ArrayList<Task>();
  }
  
  public static void create(Task task) {
  }
  
  public static void delete(Long id) {
  }
    
}
//#model-base-task
