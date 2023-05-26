package javaguide.di.guice.classfield;

//#class-field-dependency-injection
import com.google.inject.Singleton;

@Singleton
public class LiveCounter implements Counter {
    public void inc(String label) {
        System.out.println("inc " + label);
    }
}
//#class-field-dependency-injection