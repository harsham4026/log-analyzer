package cloud.fan

import scala.sys.process._

/**
 * Created by cloud on 3/26/14.
 */
object analyzeTopLog extends LogAnalyzer {

  val group: String = "top"
  val command: String = "top -b -d 10 -p "

  def apply(nodeType: String, node: String, logDir: String, process: String, reTryCount: Int = 20) {
    val pid = Seq("ssh", node, "ps aux|grep "+process+"|grep -v grep|tail -n 1|awk '{print $2}'").!!.trim
    if (pid.matches("\\d+")) {
      charts += new Chart("cpu", s"CPU utilization of $process", percentage, Array("cpu"))
      charts += new Chart("memory", s"memory utilization of $process", percentage, Array("memory"))
      analyzeLog.initCharts(nodeType, node, group, charts.toArray)
      val logIterator = analyzeLog.getLogContentIterator(command + pid, node, logDir)
      logIterator.map(_.trim).filter(_.matches("\\d+.+")).foreach {line =>
        val data = line.split("\\s+")
        ChartSender.sendData(nodeType, node, group, charts.head.name, Array(data(8)))
        ChartSender.sendData(nodeType, node, group, charts.last.name, Array(data(9)))
      }
    } else {
      if (reTryCount > 0) {
        Thread.sleep(10000)
        apply(nodeType, node, logDir, process, reTryCount - 1)
      } else {
        System.err.println(s"process $process can not found at remote server $node!")
      }
    }
  }

  val pattern = s"$group:([a-zA-Z]+)".r
}
