package play.test;

/**
 * provides a way to execute an integration test
 * example:
 * {{{
 * package test;
 * class IntegrationTest extends IntegrationTest {
 *    test = new Runnable {
 *              public void run() {
 *              //your test comes hre
 *              }}
 * }
 * }}}
 **/
public abstract class IntegrationTestRunner {

  public static Runnable test = null;

  public static void main(String[] args) {

    if (test != null) throw new RuntimeException("you should populate `test` first");

    play.api.test.IntegrationTestRunner runner = new play.api.test.IntegrationTestRunner() {
    public void test() {test.run();} 
  };
    runner.main(new String[]{});
  }

}
