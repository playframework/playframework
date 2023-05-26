package javaguide.di.guice.classfield;

//#class-field-dependency-injection
@ImplementedBy(LiveCounter.class)
interface Counter {
    public void inc(String label);
}
//#class-field-dependency-injection
