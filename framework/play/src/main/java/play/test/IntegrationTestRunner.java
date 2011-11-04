package play.test;

abstract class IntegrationTestRunner {

  static Runnable executable = null;

  public static void main(String[] args) {

    if (executable != null) throw new RuntimeException("you should call executable first");

    play.test.api.IntegrationTestRunner runner = new play.test.api.IntegrationTestRunner() {
    public void run() {executable.run();} 
  };
    runner.main(new String[]{});
  }

}
