/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package play.microbenchmark

/**
 * A custom runner to run the Play JMH benchmarks.
 *
 * This runner allows us to add an agent to the JMH forked benchmark
 * JVM arguments. We do this by reading a system property to get the agent
 * JAR location, then adding an extra command line option to the JMH arguments.
 */
object PlayJmhRunner {

  def main(args: Array[String]): Unit = {
    val jettyAnlpAgentJarPath = System.getProperty("jetty.anlp.agent.jar")
    val extraArgs = Array("-jvmArgsPrepend", s"-javaagent:$jettyAnlpAgentJarPath")
    org.openjdk.jmh.Main.main(args ++ extraArgs)
  }

}
