import org.junit.*;

import java.util.*;

import play.db.jpa.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

import models.*;

public class ModelTest {
    
    private String formatted(Date date) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    @Test
    public void findById() {
        running(fakeApplication(), new Runnable() {
           public void run() {
               JPA.withTransaction(new play.libs.F.Callback0() {
                   public void invoke() {
                       Computer macintosh = Computer.findById(21l);
                       assertThat(macintosh.name).isEqualTo("Macintosh");
                       assertThat(formatted(macintosh.introduced)).isEqualTo("1984-01-24");
                   }
               });
           }
        });
    }
    
    @Test
    public void pagination() {
        running(fakeApplication(inMemoryDatabase()), new Runnable() {
           public void run() {
               JPA.withTransaction(new play.libs.F.Callback0() {
                   public void invoke() {
                       Computer.Page computers = Computer.page(1, 20, "name", "ASC", "");
                       assertThat(computers.getTotalRowCount()).isEqualTo(574);
                       assertThat(computers.getList().size()).isEqualTo(20);
                   }
               });
           }
        });
    }
    
}
